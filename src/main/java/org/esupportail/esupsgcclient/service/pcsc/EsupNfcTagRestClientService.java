package org.esupportail.esupsgcclient.service.pcsc;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Component
public class EsupNfcTagRestClientService {

	private final static Logger log = Logger.getLogger(EsupNfcTagRestClientService.class);

	@Resource
	RestTemplate restTemplate;

	@Resource
	AppConfig appConfig;

	@Resource
	AppSession appSession;

    public  NfcResultBean getApdu(String csn, String result) throws Exception {
			String url = String.format("%s/desfire-ws/?result=%s&numeroId=%s&cardId=%s", appConfig.getEsupNfcTagServerUrl(), result, appSession.getNumeroId(), csn);
			try {
				return restTemplate.getForObject(url, NfcResultBean.class);
			} catch(HttpClientErrorException clientEx) {
				throw new EncodingException("Exception during calling esupnfcTagServer", clientEx);
			}
    }

	public String csnNfcComm(String csn) throws EncodingException, PcscException {
		CsnMessageBean nfcMsg = new CsnMessageBean();
		nfcMsg.setNumeroId(appSession.getNumeroId());
		nfcMsg.setCsn(csn);
		ObjectMapper mapper = new ObjectMapper();
		String jsonInString = null;
		String url =  appConfig.getEsupNfcTagServerUrl() + "/csn-ws";
		String nfcComm;
		try{
			jsonInString = mapper.writeValueAsString(nfcMsg);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<String>(jsonInString, headers);
			nfcComm = restTemplate.postForObject(url, entity, String.class);
		}catch (Exception e) {
			throw new EncodingException("rest call error for : " + url, e);
		}
		return nfcComm;
	}

}
