package org.esupportail.esupsgcclient.service.printer.evolis;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.utils.Utils;

public class EvolisHeartbeatTaskService extends Service<Void> {

    final static Logger log = Logger.getLogger(EvolisHeartbeatTaskService.class);

    public static SimpleBooleanProperty printerReady = new SimpleBooleanProperty();


    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                while(true) {
                    try {
                        EvolisResponse status = EvolisPrinterService.getPrinterStatus();
                        printerReady.setValue(true);
                        updateTitle(status.getResult());
                    } catch(Exception e) {
                        log.debug(e);
                    }
                    Utils.sleep(5000);
                }
            }
        };
    }
}
