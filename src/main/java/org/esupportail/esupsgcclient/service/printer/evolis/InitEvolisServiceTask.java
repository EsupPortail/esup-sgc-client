package org.esupportail.esupsgcclient.service.printer.evolis;

import javafx.application.Platform;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.EncodingException;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.ui.MainController;
import org.esupportail.esupsgcclient.utils.Utils;

public class InitEvolisServiceTask extends Task<Void> {

    final static Logger log = Logger.getLogger(InitEvolisServiceTask.class);

    public static SimpleBooleanProperty printerReady = new SimpleBooleanProperty();

    @Override
    protected Void call() {
        while(true) {
            printerReady.setValue(EvolisPrinterService.initSocket(false));
            Utils.sleep(1000);
        }
    }

}
