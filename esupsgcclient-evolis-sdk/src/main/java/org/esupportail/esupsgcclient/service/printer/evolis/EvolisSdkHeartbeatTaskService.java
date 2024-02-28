package org.esupportail.esupsgcclient.service.printer.evolis;

import javax.annotation.Resource;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;


@Component
public class EvolisSdkHeartbeatTaskService extends Service<Void> {

    final static Logger log = Logger.getLogger(EvolisSdkHeartbeatTaskService.class);

    @Resource
    EvolisSdkPrinterService evolisPrinterService;

    @Resource
    AppSession appSession;

    String lastPrinterStatus = "...";

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while(true) {
                    try {
                        String printerStatus = evolisPrinterService.getPrinterStatus();
                        if(printerStatus.contains("PRINTER_READY")) {
                            if(!appSession.isPrinterReady()) {
                                evolisPrinterService.init();
                                appSession.setPrinterReady(true);
                            }
                        }
                        if(printerStatus!=null && !printerStatus.equals(lastPrinterStatus)) {
                            lastPrinterStatus = printerStatus;
                            updateTitle("Statut Evolis : " + lastPrinterStatus);
                        }
                    } catch(Exception e) {
                        appSession.setPrinterReady(false);
                        log.trace("pb with evolisPrinterService ...", e);
                        if( e.getMessage()!=null && ! e.getMessage().equals(lastPrinterStatus)) {
                            lastPrinterStatus = e.getMessage();
                            updateTitle("Statut Evolis : " + lastPrinterStatus);
                        }
                    }
                    Utils.sleep(2000);
                }
            }
        };
    }
}
