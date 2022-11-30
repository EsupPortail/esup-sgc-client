package org.esupportail.esupsgcclient.service.sgc;

import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.tasks.EvolisTask;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;

@Component
public class EsupSgcLongPollService {
    final static Logger log = LoggerFactory.getLogger(EsupSgcLongPollService.class);

    @Resource
    AppConfig appConfig;
    @Resource
    AppSession appSession;
    RestTemplate restTemplate;
    public EsupSgcLongPollService() {
        HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
        httpRequestFactory.setConnectionRequestTimeout(300000);
        httpRequestFactory.setConnectTimeout(300000);
        httpRequestFactory.setReadTimeout(300000);
        restTemplate = new RestTemplate(httpRequestFactory);
    }

    public String getQrCode(EvolisTask evolisTask) {
        while (true) {
            if(evolisTask.isCancelled()) {
                throw new RuntimeException("EvolisTask is cancelled");
            }
            String sgcAuthToken = appSession.getSgcAuthToken();
            if (sgcAuthToken != null && !sgcAuthToken.equals("") && !"undefined".equals(sgcAuthToken) && !"null".equals(sgcAuthToken)) {
                try {
                    log.debug("Call " + appConfig.getEsupSgcUrl() + "/wsrest/nfc/qrcode2edit?authToken=" + sgcAuthToken);
                    String qrcode = restTemplate.getForObject(appConfig.getEsupSgcUrl() + "/wsrest/nfc/qrcode2edit?authToken=" + sgcAuthToken, String.class);
                    if (qrcode != null) {
                        log.debug("qrcode : " + qrcode);
                        if("stop".equals(qrcode)) {
                            throw new RuntimeException("Un esup-sg-client avec le même utilisateur est déjà démarré ??");
                        }
                        return qrcode;
                    }
                } catch (ResourceAccessException e) {
                    log.debug("timeout ... we recall esup-sgc in 2 sec");
                    Utils.sleep(2000);
                }
            } else {
                Utils.sleep(1000);
            }
        }
    }

}
