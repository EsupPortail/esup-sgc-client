package org.esupportail.esupsgcclient.service.pcsc;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
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

    String lastTerminalName = "...";

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while(true) {
                    try {
                        String terminalName =  encodingService.getTerminalName();
                        log.debug("terminalName ...");
                        appSession.setNfcReady(terminalName!=null);
                        if(terminalName!=null && !terminalName.equals(lastTerminalName)) {
                           lastTerminalName = terminalName;
                           updateTitle("Nom du terminal NFC : " + lastTerminalName);
                        }
                    } catch(Exception e) {
                        appSession.setNfcReady(false);
                        log.debug("pb with nfc ...", e);
                    }
                    Utils.sleep(5000);
                }
            }
        };
    }
}
