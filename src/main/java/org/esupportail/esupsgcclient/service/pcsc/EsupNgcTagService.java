package org.esupportail.esupsgcclient.service.pcsc;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.esupportail.esupsgcclient.domain.NfcResultBean;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

public class EsupNgcTagService {

	private final static Logger log = Logger.getLogger(EsupNgcTagService.class);
	private static RestTemplate restTemplate =  new RestTemplate(Utils.clientHttpRequestFactory());

    public static NfcResultBean getApdu(String csn, String result) throws Exception, EncodingException, PcscException {
			String url = String.format("%s/desfire-ws/?result=%s&numeroId=%s&cardId=%s", EsupSGCClientApplication.esupNfcTagServerUrl, result, EsupSGCClientApplication.numeroId, csn);
			try {
				return restTemplate.getForObject(url, NfcResultBean.class);
			} catch(HttpClientErrorException clientEx) {
				throw new EncodingException("Exception during calling esupnfcTagServer", clientEx);
			}
    }

}
