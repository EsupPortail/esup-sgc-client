package org.esupportail.esupsgcclient.service.printer.zebra;

import javax.annotation.Resource;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;


@Component
public class ZebraHeartbeatTaskService extends Service<Void> {

    final static Logger log = Logger.getLogger(ZebraHeartbeatTaskService.class);

    @Resource
    ZebraPrinterService zebraPrinterService;

    @Resource
    AppSession appSession;

    String lastPrinterStatus = "...";

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call()  {
                while(true) {
                    try {
                        String printerStatus = zebraPrinterService.getStatus();
                        log.info(String.format("Zebra printer status : %s", printerStatus));
                        if(printerStatus==null ) {
                            zebraPrinterService.init();
                        } else if(!printerStatus.equals(lastPrinterStatus)) {
                            if(!printerStatus.contains("TODO")) {
                                appSession.setPrinterReady(true);
                            }
                            lastPrinterStatus = printerStatus;
                            updateTitle("Statut Zebra : " + lastPrinterStatus);
                        }
                    } catch(Exception e) {
                        appSession.setPrinterReady(false);
                        log.trace("pb with zebraPrinterService ...", e);
                        if( e.getMessage()!=null && ! e.getMessage().equals(lastPrinterStatus)) {
                            lastPrinterStatus = e.getMessage();
                            updateTitle("Statut Zebra : " + lastPrinterStatus);
                        }
                    }
                    Utils.sleep(2000);
                }
            }
        };
    }
}
