package org.esupportail.esupsgcclient.service.sgc;

import javax.annotation.Resource;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.AppVersion;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;


@Component
public class EsupSgcHeartbeatService extends Service<Void> {

    final static Logger log = LoggerFactory.getLogger(EsupSgcHeartbeatService.class);

    @Resource
    EsupSgcRestClientService esupSgcRestClientService;

    @Resource
    AppVersion appVersion;

    EsupSgcPrinterService esupSgcPrinterService;

    public void setEsupSgcPrinterService(EsupSgcPrinterService esupSgcPrinterService) {
        this.esupSgcPrinterService = esupSgcPrinterService;
    }

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                String encodePrintHeartbeat = "OK";
                while (!this.isCancelled() && encodePrintHeartbeat!=null) {
                    try {
                        String maintenanceInfo = String.format("Esup-SGC-Client %s - compilé lé %s\n%s", appVersion.getVersion(), appVersion.getBuildDate(), esupSgcPrinterService.getMaintenanceInfo());
                        log.info("encodePrintHeartbeat - printer maintenanceInfo : " + maintenanceInfo);
                        encodePrintHeartbeat = esupSgcRestClientService.postEncodePrintHeartbeat(maintenanceInfo);
                    } catch (ResourceAccessException e) {
                        log.debug("ResourceAccessException - Read timed out ? ... wait 2 sec - " + e.getMessage());
                        Utils.sleep(2000);
                    } catch (Exception e) {
                        log.info("EsupSgcHeartbeatService failed ... wait 2 sec", e);
                        Utils.sleep(2000);
                    }
                }
                log.warn("EsupSgcHeartbeatService stopped");
                return null;
            }
        };
    }
}
