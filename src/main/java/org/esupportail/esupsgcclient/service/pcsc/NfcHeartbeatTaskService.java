package org.esupportail.esupsgcclient.service.pcsc;

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
public class NfcHeartbeatTaskService extends Service<Void> {

    final static Logger log = Logger.getLogger(NfcHeartbeatTaskService.class);

    @Resource
    EncodingService encodingService;

    @Resource
    AppSession appSession;

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while(true) {
                    try {
                        String terminalName =  encodingService.getTerminalName();
                        appSession.setNfcReady(terminalName!=null);
                        updateTitle(terminalName!=null ? terminalName : "...");
                    } catch(Exception e) {
                        log.debug("b with nfc ...", e);
                    }
                    Utils.sleep(5000);
                }
            }
        };
    }
}
