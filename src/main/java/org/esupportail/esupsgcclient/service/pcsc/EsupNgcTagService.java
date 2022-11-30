package org.esupportail.esupsgcclient.service.pcsc;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.EsupSgcClientApplication;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Component
public class EsupNgcTagService {

	private final static Logger log = Logger.getLogger(EsupNgcTagService.class);
	private static RestTemplate restTemplate =  new RestTemplate(Utils.clientHttpRequestFactory());

	@Resource
	AppConfig appConfig;

	@Resource
	AppSession appSession;

    public  NfcResultBean getApdu(String csn, String result) throws Exception, EncodingException, PcscException {
			String url = String.format("%s/desfire-ws/?result=%s&numeroId=%s&cardId=%s", appConfig.getEsupNfcTagServerUrl(), result, appSession.getNumeroId(), csn);
			try {
				return restTemplate.getForObject(url, NfcResultBean.class);
			} catch(HttpClientErrorException clientEx) {
				throw new EncodingException("Exception during calling esupnfcTagServer", clientEx);
			}
    }

}
