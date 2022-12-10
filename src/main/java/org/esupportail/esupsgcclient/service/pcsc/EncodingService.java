package org.esupportail.esupsgcclient.service.pcsc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.smartcardio.CardException;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.service.SgcCheckException;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteRunExe;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.tasks.EvolisReadNfcTask;
import org.esupportail.esupsgcclient.tasks.EvolisTask;
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

@Component
public class EncodingService {
	private final static Logger log = Logger.getLogger(EncodingService.class);

	public enum BmpType {color, black}
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
			String cardTerminalName = PcscUsbService.connection();
			log.debug("cardTerminal : " + cardTerminalName);
			return true;
		} catch (CardException e) {
			log.trace("pcsc connection error : " + e.getMessage());
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
			String csn = PcscUsbService.byteArrayToHexString(PcscUsbService.hexStringToByteArray(PcscUsbService.getCardId()));
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
				log.error(e);
				throw new SgcCheckException("SGC select error : " + e.getResponseBodyAsString());
			}
		} catch (HttpServerErrorException e) {
			log.error(e);
			throw new SgcCheckException("SGC select error : Web Server Error " + e.getResponseBodyAsString());
		} catch (Exception e) {
			log.error(e);
			throw new SgcCheckException("SGC select error", e);
		}
	}

	public String getBmpAsBase64(String qrcode, BmpType bmpType) {
		String bpmEsupSgcUrl = String.format("%s/wsrest/nfc/card-bmp-b64?authToken=%s&qrcode=%s&type=%s", appConfig.getEsupSgcUrl(), appSession.getSgcAuthToken(), qrcode, bmpType);
		String bmpAsBase64 = restTemplate.getForObject(bpmEsupSgcUrl, String.class);
		return bmpAsBase64;
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
			log.error(e);
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
			return PcscUsbService.waitForCardAbsent(timeout);
		} catch (CardException e) {
			return false;
		}
	}


	public boolean waitForCardPresent(long timeout) {
		try {
			return PcscUsbService.waitForCardPresent(timeout);
		} catch (CardException e) {
			return false;
		}
	}

	public void pcscDisconnect() throws PcscException {
		try {
			PcscUsbService.disconnect();
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
		log.info("Encoding : Start");
		String result = "";
		while (true) {
			log.info("RAPDU : " + result);
			NfcResultBean nfcResultBean = esupNfcTagRestClientService.getApdu(csn, result);
			log.info("SAPDU : " + nfcResultBean.getFullApdu());
			if (nfcResultBean.getFullApdu() != null) {
				if (!"END".equals(nfcResultBean.getFullApdu())) {
					try {
						result = PcscUsbService.sendAPDU(nfcResultBean.getFullApdu());
					} catch (CardException e) {
						throw new PcscException("pcsc send apdu error", e);
					}
				} else {
					log.info("Encoding  : OK");
					encodeCnousIfNeeded(csn);
					return nfcResultBean;
				}
			} else {
				throw new EncodingException("NFC APDU gived by nfctag is null ?!");
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
				log.warn("cnous csv send : Failed for csn " + csn, null);
			}
		} else {
			log.info("Cnous Encoding :  Skipped");
			log.info("Encodage terminé");
		}
	}

	public String getTerminalName() throws CardException, PcscException {
		return PcscUsbService.getTerminalName();
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
