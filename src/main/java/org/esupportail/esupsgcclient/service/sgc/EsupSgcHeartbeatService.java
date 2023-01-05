package org.esupportail.esupsgcclient.service.sgc;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class EsupSgcHeartbeatService extends Service<Void> {

    final static Logger log = Logger.getLogger(EsupSgcHeartbeatService.class);

    @Resource
    EsupSgcRestClientService esupSgcRestClientService;

    @Resource
    EvolisPrinterService evolisPrinterService;

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                String encodePrintHeartbeat = "OK";
                while (!this.isCancelled() && encodePrintHeartbeat!=null) {
                    try {
                        String maintenanceInfo = evolisPrinterService.getNextCleaningSteps().getResult();
                        log.info("encodePrintHeartbeat - printer maintenanceInfo : " + maintenanceInfo);
                        encodePrintHeartbeat = esupSgcRestClientService.postEncodePrintHeartbeat(maintenanceInfo);
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
