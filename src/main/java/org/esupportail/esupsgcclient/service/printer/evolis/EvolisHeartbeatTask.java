package org.esupportail.esupsgcclient.service.printer.evolis;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.utils.Utils;

public class EvolisHeartbeatTask extends Task<Void> {

    final static Logger log = Logger.getLogger(EvolisHeartbeatTask.class);

    public static SimpleBooleanProperty printerReady = new SimpleBooleanProperty();

    @Override
    protected Void call() {
        while(true) {
            printerReady.setValue(EvolisPrinterService.initSocket(false));
            Utils.sleep(1000);
        }
    }

}
