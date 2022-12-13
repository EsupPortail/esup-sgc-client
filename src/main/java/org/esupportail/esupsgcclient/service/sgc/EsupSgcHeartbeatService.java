package org.esupportail.esupsgcclient.service.sgc;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisResponse;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class EsupSgcHeartbeatService extends Service<Void> {

    final static Logger log = Logger.getLogger(EsupSgcHeartbeatService.class);

    @Resource
    EsupSgcRestClientService esupSgcRestClientService;

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() {
                while (!this.isCancelled()) {
                    try {
                        log.info("encodePrintHeartbeat");
                        esupSgcRestClientService.getEncodePrintHeartbeat();
                    } catch (Exception e) {
                        log.info("EsupSgcHeartbeatService failed", e);
                    }
                }
                return null;
            }
        };
    }
}
