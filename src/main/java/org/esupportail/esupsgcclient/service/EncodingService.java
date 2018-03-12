package org.esupportail.esupsgcclient.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import javax.smartcardio.CardException;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.domain.NfcResultBean;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteRunExe;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.pcsc.PcscUsbService;
import org.esupportail.esupsgcclient.ui.EsupSGCJFrame;
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

public class EncodingService {

	private final static Logger log = Logger.getLogger(EncodingService.class);
	
	public String authToken =  "aaaaaaaaaaaaaaaaaaaaaaaaaaaaa";
	public String esupNfcTagServerUrl = "https://esup-nfc-tag-test.univ-ville.fr";
	public String sgcUrl = "https://esup-sgc-test.univ-ville.fr";
	public String numeroId = "0000000000000000000";
	public String eppnInit = "user@univ-ville.fr";
	public Boolean encodeCnous = false;
	
	public static String pathToExe = "c:\\cnousApi\\";
	public static String csvPath = "c:\\cnousApi\\csv_out.csv";
	
	private RestTemplate restTemplate =  new RestTemplate(Utils.clientHttpRequestFactory());
	
	private boolean cnousOK = false;
	private static CnousFournisseurCarteRunExe cnousFournisseurCarteRunExe = new CnousFournisseurCarteRunExe(pathToExe);
	private static PcscUsbService pcscUsbService = new PcscUsbService();
	
	public void init(String[] args){
		
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
			if(cnousFournisseurCarteRunExe.isReady()) cnousOK=true;
		}

	}
	
	public void pcscConnection() throws PcscException{
		try {
			String cardTerminalName = pcscUsbService.connection();
			//esupSGCClientJFrame.addLogText("cardTerminal : " + cardTerminalName);
			log.debug("cardTerminal : " + cardTerminalName);
		} catch (CardException e) {
			log.error("pcsc connection error", e);
			throw new PcscException("pcsc connection error", e);
		}
	}
	
	public String readCsn() throws PcscException{
		try {
			String csn = pcscUsbService.byteArrayToHexString(pcscUsbService.hexStringToByteArray(pcscUsbService.getCardId()));
			log.info("csn : "+csn);
			return csn;
		} catch (CardException e) {
			log.error("csn read error" + e);
			throw new PcscException("csn read error", e);
		}
	}
	
	public void checkBeforeEncoding(String qrcode, String csn) throws EncodingException{
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
				throw new EncodingException("Card for " + qrcode + " not found, please check its state in SGC web application.");	

			} else {
				log.error(e);
				throw new EncodingException("SGC select error : " + e.getResponseBodyAsString());
			}
		} catch (HttpServerErrorException e) {
			log.error(e);
			throw new EncodingException("SGC select error : Web Server Error " + e.getResponseBodyAsString());
		} catch (Exception e){
			log.error(e);
			throw new EncodingException("SGC select error", e);
		}
	}
	
	public String appsEncoding(String cardId, EsupSGCJFrame esupSGCJFrame) throws EncodingException, PcscException {
		String urlTest = esupNfcTagServerUrl + "/desfire-ws/?result=&numeroId="+numeroId+"&cardId="+cardId;
		NfcResultBean nfcResultBean;
		try{
			nfcResultBean = restTemplate.getForObject(urlTest, NfcResultBean.class);
		}catch (Exception e) {
			throw new EncodingException("rest call error for : " + urlTest + " - " + e);
		}
		log.info("Rest call : " + urlTest);
		log.info("Result of rest call :" + nfcResultBean);
		if(nfcResultBean.getFullApdu()!=null) {
			log.info("Encoding : Start");
			String result = "";
			while(true){
				log.info("RAPDU : "+ result);
				String url = esupNfcTagServerUrl + "/desfire-ws/?result="+ result +"&numeroId="+numeroId+"&cardId="+cardId;
				nfcResultBean = restTemplate.getForObject(url, NfcResultBean.class);
				log.info("SAPDU : "+ nfcResultBean.getFullApdu());
				if(nfcResultBean.getFullApdu()!=null){
				if(!"END".equals(nfcResultBean.getFullApdu()) ) {
					try {
						result = pcscUsbService.sendAPDU(nfcResultBean.getFullApdu());
						esupSGCJFrame.addLogText(".");
					} catch (CardException e) {
						throw new PcscException("pcsc send apdu error", e);
					}
				} else {
					esupSGCJFrame.addLogTextLn(".");
					log.info("Encoding  : OK");
					return nfcResultBean.getFullApdu();
				}
				}else{
					esupSGCJFrame.addLogTextLn(".");
					throw new EncodingException("return is null");
				}
			}
		} else {
			return nfcResultBean.getFullApdu();
		}
	}
	
	public boolean cnousEncoding(String cardId) throws CnousFournisseurCarteException {
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
	
	public boolean sendCnousCsv(String csn) throws EncodingException{
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

	public boolean delCnousCsv() throws EncodingException{
		try{
			File file = new File(csvPath);
			return file.delete();
		}catch (Exception e) {
			throw new EncodingException("Probléme de suppression du CSV", e);
		}
	}
	
	public boolean isCnousOK(){
		return cnousOK;
	}
	
	public boolean pcscCardOnTerminal(){
		try {
			return pcscUsbService.isCardOnTerminal();
		} catch (CardException e) {
			return false;
		}
	}
	
	public void pcscDisconnect() throws PcscException{
		try {
			pcscUsbService.disconnect();
		} catch (PcscException e) {
			throw new PcscException(e.getMessage(), e);
		}
		Utils.sleep(1000);
	}
	
	@SuppressWarnings("unchecked")
	private Map<String, String> getEppnAndNumeroId(String authToken, String sgcUrl) {
		String url = sgcUrl + "/wsrest/nfc/eppnAndNumeroId?authToken=" + authToken;
		Map<String, String> eppnAndNumeroId  = restTemplate.getForObject(url, Map.class);
		return eppnAndNumeroId;
	}
	
}
