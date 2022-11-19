package org.esupportail.esupsgcclient.task;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.utils.Utils;

// TODO
public class SgcNfcHeartbeatTask extends Task<Void> {

    final static Logger log = Logger.getLogger(SgcNfcHeartbeatTask.class);

    public static SimpleBooleanProperty sgcReady = new SimpleBooleanProperty();

    public static SimpleBooleanProperty nfcReady = new SimpleBooleanProperty();

    @Override
    protected Void call() {
        while(true) {
            // TODO : url heartbeat on sgc & nfc servers
            Utils.sleep(1000);
        }
    }

}
