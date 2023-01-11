package org.esupportail.esupsgcclient.service.sgc;

import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.tasks.EvolisEncodePrintTask;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import javax.annotation.Resource;
import java.util.HashMap;
import java.util.Map;

@Component
public class EsupSgcRestClientService {
    final static Logger log = LoggerFactory.getLogger(EsupSgcRestClientService.class);

    @Resource
    AppConfig appConfig;

    @Resource
    AppSession appSession;

    @Resource
    RestTemplate restTemplate;

    public String getQrCode(EsupSgcTask esupSgcTask) {
        while (true) {
            if(esupSgcTask.isCancelled()) {
                throw new RuntimeException("EvolisTask is cancelled");
            }
            String sgcAuthToken = appSession.getSgcAuthToken();
            if (sgcAuthToken != null && !sgcAuthToken.equals("") && !"undefined".equals(sgcAuthToken) && !"null".equals(sgcAuthToken)) {
                try {
                    String sgcUrl = appConfig.getEsupSgcUrl() + "/wsrest/nfc/qrcode2edit?authToken=" + sgcAuthToken;
                    log.debug("Call " + sgcUrl);
                    esupSgcTask.updateTitle4thisTask("Call " + sgcUrl);
                    String qrcode = restTemplate.getForObject(sgcUrl, String.class);
                    if (qrcode != null) {
                        log.debug("qrcode : " + qrcode);
                        if("stop".equals(qrcode)) {
                            throw new RuntimeException("Un esup-sgc-client avec le même utilisateur vient d'être (re)lancé ?!");
                        }
                        return qrcode;
                    } else {
                        log.info("Pas de carte à éditer - on relance l'appel à ESUP-SGC dans 3 secondes.");
                        Utils.sleep(3000);
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

    public void setCardEncodedPrinted(String csn, String qrcode) {
        String sgcAuthToken = appSession.getSgcAuthToken();
        log.debug("Call " + appConfig.getEsupSgcUrl() + "/wsrest/nfc/card-encoded-printed?authToken=" + sgcAuthToken);
        Map<String, String> qrcodeAndCsn = new HashMap<>();
        qrcodeAndCsn.put("csn", csn);
        qrcodeAndCsn.put("qrcode", qrcode);
        String result = restTemplate.postForObject(appConfig.getEsupSgcUrl() + "/wsrest/nfc/card-encoded-printed?authToken=" + sgcAuthToken, qrcodeAndCsn, String.class);
        log.info("result of setCardEncodedPrinted of " + qrcodeAndCsn + " : " + result);
    }

    public String postEncodePrintHeartbeat(String maintenanceInfo) {
        String sgcAuthToken = appSession.getSgcAuthToken();
        while (sgcAuthToken == null || sgcAuthToken.equals("") || "undefined".equals(sgcAuthToken) || "null".equals(sgcAuthToken)) {
            Utils.sleep(1000);
        }
        String sgcUrl = appConfig.getEsupSgcUrl() + "/wsrest/nfc/encodePrintHeartbeat?authToken=" + sgcAuthToken;
        log.debug("Call " + sgcUrl);
        String esupSgcHeartbeatResponse = restTemplate.postForObject(sgcUrl, maintenanceInfo, String.class);
        log.debug ("Esup Sgc Heartbeat Response : " + esupSgcHeartbeatResponse);
        return esupSgcHeartbeatResponse;
    }
}
