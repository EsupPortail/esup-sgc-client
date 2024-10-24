package org.esupportail.esupsgcclient.service.pcsc;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.Resource;

import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;


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
				NfcResultBean nfcResultBean = restTemplate.getForObject(url, NfcResultBean.class);
				if(nfcResultBean.getFullApdu() == null) {
					throw new EncodingException(String.format("NFC APDU gived by esup-nfc-tag-server is null ?!\n### URL ###\n%s", url));
				}
				return nfcResultBean;
			} catch(HttpClientErrorException clientEx) {
				throw new EncodingException("Exception during calling esupnfcTagServer : " + url, clientEx);
			}
    }

	public NfcResultBean csnNfcComm(String csn) throws EncodingException, PcscException {
		CsnMessageBean nfcMsg = new CsnMessageBean();
		nfcMsg.setNumeroId(appSession.getNumeroId());
		nfcMsg.setCsn(csn);
		ObjectMapper mapper = new ObjectMapper();
		String jsonInString = null;
		String url =  appConfig.getEsupNfcTagServerUrl() + "/csn-ws";
		NfcResultBean nfcResultBean  = null;
		try{
			jsonInString = mapper.writeValueAsString(nfcMsg);
			HttpHeaders headers = new HttpHeaders();
			headers.setContentType(MediaType.APPLICATION_JSON);
			HttpEntity<String> entity = new HttpEntity<String>(jsonInString, headers);
			nfcResultBean = restTemplate.postForObject(url, entity, NfcResultBean.class);
		}catch (Exception e) {
			throw new EncodingException("rest call error for : " + url, e);
		}
		return nfcResultBean;
	}

}
