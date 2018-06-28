package org.esupportail.esupsgcclient.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.smartcardio.CardException;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteRunExe;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.pcsc.PcscUsbService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.core.io.FileSystemResource;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

@SuppressWarnings("restriction")
public class EncodingService {

	private final static Logger log = Logger.getLogger(EncodingService.class);
	
	private static RestTemplate restTemplate =  new RestTemplate(Utils.clientHttpRequestFactory());
	
	private static boolean encodeCnous = false;
	private static String pathToExe = "c:\\cnousApi\\";
	private static String csvPath = "c:\\cnousApi\\csv_out.csv";
	private static CnousFournisseurCarteRunExe cnousFournisseurCarteRunExe;
	private static boolean cnousOK = false;
		
	public static String esupNfcTagServerUrl = "https://esup-nfc-tag.univ-ville.fr";
	private static String sgcUrl = "https://esup-sgc.univ-ville.fr";
	public static String numeroId = "0000000000000000000";
	private static String eppnInit = "user@univ-ville.fr";
	
	private static String authToken = "aaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
	
	public static void init(String[] args) throws EncodingException, PcscException, CnousFournisseurCarteException {
		
		PcscUsbService.init();
		
		if(args.length>0) {
			authToken =  args[0];
			esupNfcTagServerUrl = args[1];
			sgcUrl = args[2];
			encodeCnous = Boolean.valueOf(args[3]);
			Map<String, String> eppnAndNumeroId = getEppnAndNumeroId(authToken, sgcUrl);
			numeroId = eppnAndNumeroId.get("numeroId");
			eppnInit = eppnAndNumeroId.get("eppnInit");
		}
		
		if(encodeCnous){
			try{
				cnousFournisseurCarteRunExe = new CnousFournisseurCarteRunExe(pathToExe);
				cnousOK = cnousFournisseurCarteRunExe.check();
			}catch(CnousFournisseurCarteException e){
				throw new CnousFournisseurCarteException(e.getMessage(), e);
			}
			if(!cnousOK) {
				throw new CnousFournisseurCarteException("Erreur cnousApi");
			}
		}

	}
	
	public static void pcscConnection() throws PcscException{
		try {
			String cardTerminalName = PcscUsbService.connection();
			log.debug("cardTerminal : " + cardTerminalName);
		} catch (CardException e) {
			throw new PcscException("pcsc connection error", e);
		}
	}
	
	public static String readCsn() throws PcscException{
		try {
			String csn = PcscUsbService.byteArrayToHexString(PcscUsbService.hexStringToByteArray(PcscUsbService.getCardId()));
			log.info("csn : "+csn);
			return csn;
		} catch (CardException e) {
			log.error("csn read error" + e);
			throw new PcscException("csn read error", e);
		}
	}
	
	public static void checkBeforeEncoding(String qrcode, String csn) throws SgcCheckException{
		HttpHeaders httpHeaders = new HttpHeaders();
		httpHeaders.add("Content-Type", MediaType.APPLICATION_JSON.toString());
		Map<String, String> requestBody = new HashMap<String, String>();
		requestBody.put("qrcode", qrcode);
		requestBody.put("csn", csn);
		HttpEntity<Map<String, String>> httpEntity = new HttpEntity<Map<String, String>>(requestBody, httpHeaders);
		String selectUrl = sgcUrl + "/wsrest/nfc/check4encode";	
		
		try {
			restTemplate.postForObject(selectUrl, httpEntity, String.class);
		} catch (HttpClientErrorException e) {
			if(HttpStatus.NOT_FOUND.equals(e.getStatusCode())) {
				log.warn("Card for " + qrcode + " not found, please check its state in SGC web application.");
				throw new SgcCheckException("Card for " + qrcode + " not found, please check its state in SGC web application.");	

			} else {
				log.error(e);
				throw new SgcCheckException("SGC select error : " + e.getResponseBodyAsString());
			}
		} catch (HttpServerErrorException e) {
			log.error(e);
			throw new SgcCheckException("SGC select error : Web Server Error " + e.getResponseBodyAsString());
		} catch (Exception e){
			log.error(e);
			throw new SgcCheckException("SGC select error " + e.getMessage(), e);
		}
	}
	
	public static boolean cnousEncoding(String cardId) throws CnousFournisseurCarteException {
		String cnousUrl = sgcUrl + "/wsrest/nfc/cnousCardId?authToken="+authToken+"&csn="+cardId;
		try{
			ResponseEntity<String> response = restTemplate.exchange(cnousUrl, HttpMethod.GET, null, String.class);
			log.debug("cnous id : " + response.getBody());
			String result = cnousFournisseurCarteRunExe.writeCard(response.getBody());
			log.debug("cnous encoding : "+result);
			if(!result.contains("false")){
				return true;
			}else{
				return false;
			}
		}catch (RestClientException e) {
			log.error(e);
			throw new CnousFournisseurCarteException("cnous error : can't get cnousId : " + e.getMessage());
		}

	}
	
	public static boolean sendCnousCsv(String csn) throws EncodingException{
		try{
			File file = new File(csvPath);
			LinkedMultiValueMap<String, Object> map = new LinkedMultiValueMap<String, Object>();
			map.add("file", new FileSystemResource(file));
	
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.MULTIPART_FORM_DATA);
			HttpEntity<LinkedMultiValueMap<String, Object>> requestEntity = new HttpEntity<LinkedMultiValueMap<String, Object>>(
					map, headers);

			ResponseEntity<String> fileSendResult = restTemplate.exchange(
				sgcUrl + "/wsrest/nfc/addCrousCsvFile?authToken=" + authToken + "&csn=" + csn,
				HttpMethod.POST, requestEntity,
				String.class);
			log.debug("csv send : " + fileSendResult.getBody());
		}catch (Exception e) {
			log.error("Erreur lors de l'envoi du CSV", e);
			throw new EncodingException("Erreur lors de l'envoi du CSV", e);
		}
		
		return true;
	}

	public static boolean delCnousCsv() throws EncodingException{
		try{
			File file = new File(csvPath);
			return file.delete();
		}catch (Exception e) {
			throw new EncodingException("Probléme de suppression du CSV", e);
		}
	}
	
	public static boolean pcscCardOnTerminal(){
		try {
			return PcscUsbService.isCardOnTerminal();
		} catch (CardException e) {
			return false;
		}
	}
	
	public static void pcscDisconnect() throws PcscException{
		try {
			PcscUsbService.disconnect();
		} catch (PcscException e) {
			throw new PcscException(e.getMessage(), e);
		}
		Utils.sleep(1000);
	}
	
	@SuppressWarnings("unchecked")
	private static Map<String, String> getEppnAndNumeroId(String authToken, String sgcUrl) {
		String url = sgcUrl + "/wsrest/nfc/eppnAndNumeroId?authToken=" + authToken;
		Map<String, String> eppnAndNumeroId  = restTemplate.getForObject(url, Map.class);
		return eppnAndNumeroId;
	}
	
	public static String getEsupNfcTagServerUrl() {
		return esupNfcTagServerUrl;
	}

	public static String getSgcUrl() {
		return sgcUrl;
	}

	public static String getNumeroId() {
		return numeroId;
	}

	public static String getEppnInit() {
		return eppnInit;
	}

	public static boolean isEncodeCnous() {
		return encodeCnous;
	}

	public static boolean isCnousOK(){
		return cnousOK;
	}
	
}
