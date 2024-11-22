package org.esupportail.esupsgcclient.service.pcsc;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.service.SgcCheckException;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteRunExe;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.LogTextAreaService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.smartcardio.CardException;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
public class EncodingService {
	private final static Logger log = LoggerFactory.getLogger(EncodingService.class);

	public enum BmpType {color, overlay, black, back}
	private String pathToExe = "c:\\cnousApi\\";
	private String csvPath = "c:\\cnousApi\\csv_out.csv";
	private CnousFournisseurCarteRunExe cnousFournisseurCarteRunExe;
	private boolean cnousOK = false;

	@Resource
	RestTemplate restTemplate;

	@Resource
	AppConfig appConfig;

	@Resource
	AppSession appSession;

	@Resource
	EsupNfcTagRestClientService esupNfcTagRestClientService;

	@Resource
	PcscUsbService pcscUsbService;

	@Resource
	LogTextAreaService logTextAreaService;

	@PostConstruct
	void init() throws CnousFournisseurCarteException {

		if (appConfig.isEncodeCnous()) {
			try {
				cnousFournisseurCarteRunExe = new CnousFournisseurCarteRunExe(pathToExe);
				cnousOK = cnousFournisseurCarteRunExe.check();
			} catch (CnousFournisseurCarteException e) {
				throw new CnousFournisseurCarteException(e.getMessage(), e);
			}
			if (!cnousOK) {
				throw new CnousFournisseurCarteException("Erreur cnousApi");
			}
		}
	}

	public boolean pcscConnection() {
		try {
			String cardTerminalName = pcscUsbService.connection();
			log.debug("cardTerminal : " + cardTerminalName);
			return true;
		} catch (CardException e) {
			log.trace("pcsc connection error : " + e.getMessage());
			// usually "No NFC reader found with card on it"
			Utils.sleep(500);
		} catch (PcscException e) {
			log.warn("pcsc connection error : " + e.getMessage());
			Utils.sleep(1000);
		}
		return false;
	}

	public void pcscConnection(EsupSgcTask esupSgcTask) {
		while(!pcscConnection()) {
			if(esupSgcTask.isCancelled()) {
				throw new RuntimeException("esupSgcTask is cancelled");
			}
			esupSgcTask.updateTitle4thisTask("En attente d'une carte sur le lecteur NFC");
			Utils.sleep(200);
		}
	}

	public String readCsn() throws PcscException {
		try {
			String csn = pcscUsbService.byteArrayToHexString(pcscUsbService.hexStringToByteArray(pcscUsbService.getCardId()));
			log.info("csn : " + csn);
			return csn;
		} catch (CardException e) {
			log.error("csn read error" + e);
			throw new PcscException("csn read error", e);
		}
	}

	public void checkBeforeEncoding(String qrcode, String csn) throws SgcCheckException {
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON.toString());
		Map<String, String> requestBody = new HashMap<String, String>();
		requestBody.put("qrcode", qrcode);
		requestBody.put("csn", csn);
		HttpEntity<Map<String, String>> httpEntity = new HttpEntity<Map<String, String>>(requestBody, httpHeaders);
		String selectUrl = appConfig.getEsupSgcUrl() + "/wsrest/nfc/check4encode";

		try {
			restTemplate.postForObject(selectUrl, httpEntity, String.class);
		} catch (HttpClientErrorException e) {
			if (HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				log.warn("Card for " + qrcode + " not found, please check its state in SGC web application.");
				throw new SgcCheckException("Card for " + qrcode + " not found, please check its state in SGC web application.");

			} else {
				log.error("Exception on checkBeforeEncoding", e);
				throw new SgcCheckException("SGC select error : " + e.getResponseBodyAsString());
			}
		} catch (HttpServerErrorException e) {
			log.error("Exception on checkBeforeEncoding", e);
			throw new SgcCheckException("SGC select error : Web Server Error " + e.getResponseBodyAsString());
		} catch (Exception e) {
			log.error("Exception on checkBeforeEncoding", e);
			throw new SgcCheckException("SGC select error", e);
		}
	}

	public String getBmpAsBase64(String qrcode, BmpType bmpType) {
		try {
			String bpmEsupSgcUrl = String.format("%s/wsrest/nfc/card-bmp-b64?authToken=%s&qrcode=%s&type=%s", appConfig.getEsupSgcUrl(), appSession.getSgcAuthToken(), qrcode, bmpType);
			log.debug("Get " + bpmEsupSgcUrl);
			String bmpAsBase64 = restTemplate.getForObject(bpmEsupSgcUrl, String.class);
			if(!EncodingService.BmpType.back.equals(bmpType) && StringUtils.isEmpty(bmpAsBase64)) {
				throw new RuntimeException("Empty bmp " + bmpType.name() + " for " + qrcode);
			}
			return bmpAsBase64;
		} catch(Throwable t) {
			if(BmpType.back.equals(bmpType)) {
				// compatibilité avec ancienne version d'ESUP-SGC qui ne retourne pas de bmp verso
				log.error("Exception with back bmp for " + qrcode + " - verso will not be printed", t);
				return "";
			} else {
				throw t;
			}
		}
	}

	public boolean cnousEncoding(String cardId) throws CnousFournisseurCarteException {
		String cnousUrl = appConfig.getEsupSgcUrl() + "/wsrest/nfc/cnousCardId?csn=" + cardId + "&authToken=" + appSession.getSgcAuthToken();
		log.info("get cnousId : " + cnousUrl);
		try {
			ResponseEntity<String> response = restTemplate.exchange(cnousUrl, HttpMethod.GET, null, String.class);
			log.debug("cnous id : " + response.getBody());
			String result = cnousFournisseurCarteRunExe.writeCard(response.getBody());
			log.debug("cnous encoding : " + result);
			if (!result.contains("false")) {
				return true;
			} else {
				return false;
			}
		} catch (RestClientException e) {
			log.error("Exception on cnousEncoding", e);
			throw new CnousFournisseurCarteException("cnous error : can't get cnousId : " + e.getMessage());
		}
	}

	public boolean sendCnousCsv(String csn) throws EncodingException {
		try {
			File file = new File(csvPath);
			LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
			map.add("file", new FileSystemResource(file));

			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
					map, headers);
			try {
				ResponseEntity<String> fileSendResult = restTemplate.exchange(
						appConfig.getEsupSgcUrl() + "/wsrest/nfc/addCrousCsvFile?&csn=" + csn + "&authToken=" + appSession.getSgcAuthToken(),
						HttpMethod.POST, requestEntity,
						String.class);
				log.debug("csv send : " + fileSendResult.getBody());
			} catch (HttpClientErrorException clientEx) {
				throw new EncodingException("Exception during calling esupSgc (adding crous csv)", clientEx);
			}
		} catch (Exception e) {
			log.error("Erreur lors de l'envoi du CSV", e);
			throw new EncodingException("Erreur lors de l'envoi du CSV", e);
		}

		return true;
	}

	public boolean delCnousCsv() throws EncodingException {
		try {
			File file = new File(csvPath);
			return file.delete();
		} catch (Exception e) {
			throw new EncodingException("Probléme de suppression du CSV", e);
		}
	}

	public boolean waitForCardAbsent(long timeout) {
		try {
			return pcscUsbService.waitForCardAbsent(timeout);
		} catch (CardException e) {
			return false;
		}
	}


	public boolean waitForCardPresent(long timeout) {
		try {
			return pcscUsbService.waitForCardPresent(timeout);
		} catch (CardException e) {
			return false;
		}
	}

	public void pcscDisconnect() throws PcscException {
		try {
			pcscUsbService.disconnect();
		} catch (PcscException e) {
			throw new PcscException(e.getMessage(), e);
		}
		Utils.sleep(1000);
	}

	public boolean isCnousOK() {
		return cnousOK;
	}

	public NfcResultBean encode(EsupSgcTask esupSgcTask, String qrcode) throws Exception {
		long start = System.currentTimeMillis();
		pcscConnection(esupSgcTask);
		String csn = readCsn();
		if(qrcode != null) {
			checkBeforeEncoding(qrcode, csn);
		}
		esupSgcTask.updateTitle4thisTask("NFC : Start");
		int k = 0;
		String result = "";
		List<String> apduStacks = new ArrayList<>();
		while (true) {
			apduStacks.add("RAPDU : " + result);
			log.trace("RAPDU : " + result);
			try {
				NfcResultBean nfcResultBean = esupNfcTagRestClientService.getApdu(csn, result);
				apduStacks.add("SAPDU : " + nfcResultBean.getFullApdu());
				log.trace("SAPDU : " + nfcResultBean.getFullApdu());
				k++;
				// hack to log progress on textarea
				logTextAreaService.appendTextNoNewLine(k % 2 == 0 ? "." : "_");
				esupSgcTask.updateProgressStep();
				if (!"END".equals(nfcResultBean.getFullApdu())) {
					try {
						result = pcscUsbService.sendAPDU(nfcResultBean.getFullApdu());
					} catch (CardException e) {
						logTextAreaService.appendTextNoNewLine("\n");
						throw new PcscException(String.format("pcsc send apdu error, \n%s\nAPDUs Stack:\n%s", e.getMessage(), StringUtils.join(apduStacks, "\n")), e);
					}
				} else {
					logTextAreaService.appendTextNoNewLine("\n");
					encodeCnousIfNeeded(csn);
					return nfcResultBean;
				}
			} catch(EncodingException e) {
				logTextAreaService.appendTextNoNewLine("\n");
				String errorMessage = String.format("%s\n### APDUs Stack ###\n%s", e.getMessage(), StringUtils.join(apduStacks, "\n"));
				throw new EncodingException(errorMessage, e);
			}
		}

	}

	public void encodeCnousIfNeeded(String csn) throws EncodingException, CnousFournisseurCarteException {
		if (isCnousOK() && appConfig.isEncodeCnous()) {
			log.info("Cnous Encoding :  Start");
			delCnousCsv();
			if (cnousEncoding(csn)) {
				log.info("cnous encoding : OK");
				if (sendCnousCsv(csn)) {
					log.info("cnous csv send : OK");
				} else {
					log.warn("Cnous csv send :  Failed");
				}
			} else {
				log.warn("cnous csv send : Failed for csn " + csn);
			}
		} else {
			log.info("Cnous Encoding :  Skipped");
			log.info("Encodage terminé");
		}
	}

	public String getTerminalName() throws CardException, PcscException {
		return pcscUsbService.getTerminalName();
	}


	public NfcResultBean encode(EsupSgcTask esupSgcTask) throws Exception {
		if(appSession.getAuthType().equals("CSN")) {
			while (!pcscConnection()) {
				if(esupSgcTask.isCancelled()) {
					throw new RuntimeException("EvolisTask is cancelled");
				}
				esupSgcTask.updateTitle4thisTask("En attente d'une carte sur le lecteur NFC");
				Utils.sleep(1000);
			}
			return esupNfcTagRestClientService.csnNfcComm(readCsn());
		} else {
			return encode(esupSgcTask, null);
		}
	}

}
