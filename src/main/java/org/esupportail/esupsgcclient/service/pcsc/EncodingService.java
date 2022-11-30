package org.esupportail.esupsgcclient.service.pcsc;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javax.smartcardio.CardException;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.EsupSgcClientApplication;
import org.esupportail.esupsgcclient.service.SgcCheckException;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteRunExe;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.ui.UiStep;
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
	private static RestTemplate restTemplate = new RestTemplate(Utils.clientHttpRequestFactory());
	private static String pathToExe = "c:\\cnousApi\\";
	private static String csvPath = "c:\\cnousApi\\csv_out.csv";
	private static CnousFournisseurCarteRunExe cnousFournisseurCarteRunExe;
	private static boolean cnousOK = false;

	@Resource
	AppConfig appConfig;

	@Resource
	AppSession appSession;

	@Resource
	EsupNgcTagService esupNgcTagService;

	@PostConstruct
	void init() throws EncodingException, PcscException, CnousFournisseurCarteException {

		PcscUsbService.init();

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

	public boolean pcscConnection() throws PcscException {
		try {
			String cardTerminalName = PcscUsbService.connection();
			log.debug("cardTerminal : " + cardTerminalName);
			return true;
		} catch (CardException e) {
			log.warn("pcsc connection error : " + e.getMessage());
		}
		return false;
	}

	public static String readCsn() throws PcscException {
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

	public void encode(String qrcode) throws Exception {
		long start = System.currentTimeMillis();
		long t;
		while (!pcscConnection()) {
			Utils.sleep(1000);
		}
		String csn = EncodingService.readCsn();
		checkBeforeEncoding(qrcode, csn);
		log.info("Encoding : Start");
		String result = "";
		while (true) {
			t = System.currentTimeMillis() - start;
			log.info("RAPDU : " + result);
			NfcResultBean nfcResultBean = esupNgcTagService.getApdu(csn, result);
			log.info("SAPDU : " + nfcResultBean.getFullApdu());
			if (nfcResultBean.getFullApdu() != null) {
				if (!"END".equals(nfcResultBean.getFullApdu())) {
					try {
						result = PcscUsbService.sendAPDU(nfcResultBean.getFullApdu());
						//updateProgress((long)this.getProgress()+1, (long)this.getTotalWork()+1);
					} catch (CardException e) {
						throw new PcscException("pcsc send apdu error", e);
					}
				} else {
					log.info("Encoding  : OK");
					encodeCnousIfNeeded(csn);
					return;
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

	public String getTerminalName() throws CardException {
		return PcscUsbService.getTerminalName();
	}
}
