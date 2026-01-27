package org.esupportail.esupsgcclient.service.printer.evolis;

import javax.annotation.Resource;

import com.evolis.sdk.CleaningInfo;
import com.evolis.sdk.RibbonInfo;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.ui.LogTextAreaService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import java.util.Date;


@Component
public class EvolisSdkHeartbeatTaskService extends Service<Void> {

    final static Logger log = LoggerFactory.getLogger(EvolisSdkHeartbeatTaskService.class);

    @Resource
    EvolisSdkPrinterService evolisPrinterService;

    @Resource
    AppSession appSession;

    @Resource
    LogTextAreaService logTextAreaService;

    String lastPrinterStatus = "...";

    @Override
    protected Task<Void> createTask() {
        return new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                RibbonInfo ribbonInfo = null;
                Date lastRibbonInfoDate = new Date();
                while(true) {
                    try {
                        String printerStatus = evolisPrinterService.getPrinterStatus();
                        if(printerStatus.contains("PRINTER_READY")) {
                            CleaningInfo cleaningInfo = evolisPrinterService.getCleaningInfo();
                            if(cleaningInfo!=null && cleaningInfo.getCardCountBeforeWarrantyLost()<10) {
                                appSession.setPrinterReady(false);
                                printerStatus = "Nettoyage nécessaire : " + cleaningInfo.getCardCountBeforeWarrantyLost() + " cartes avant perte de garantie";
                            } else {
                                // check ribbon only if printer is ready and only each hour to avoid too much requests
                                // -> to avoid overuse of nfc chip of the ribbon
                                // -> to avoid to burn out the nfc chip of the ribbon
                                if (ribbonInfo == null || (lastRibbonInfoDate.getTime() + 1000 * 3600) < new Date().getTime()) {
                                    ribbonInfo = evolisPrinterService.getRibbonInfo();
                                    lastRibbonInfoDate = new Date();
                                }
                                if (ribbonInfo.getRemaining() < 1) {
                                    appSession.setPrinterReady(false);
                                    printerStatus = "Plus de ruban, merci de le changer";
                                } else if (!appSession.isPrinterReady()) {
                                    evolisPrinterService.init();
                                    appSession.setPrinterReady(true);
                                }
                            }
                        }
                        if(!printerStatus.equals(lastPrinterStatus)) {
                            lastPrinterStatus = printerStatus;
                            updateTitle("Statut Evolis : " + lastPrinterStatus);
                            if(!appSession.isPrinterReady()) {
                                logTextAreaService.setInfoText("Imprimante Evolis non prête : " + lastPrinterStatus, "alert-danger");
                            }
                        }
                    } catch(Exception e) {
                        appSession.setPrinterReady(false);
                        log.trace("pb with evolisPrinterService ...", e);
                        if( e.getMessage()!=null && ! e.getMessage().equals(lastPrinterStatus)) {
                            lastPrinterStatus = e.getMessage();
                            updateTitle("Statut Evolis : " + lastPrinterStatus);
                            logTextAreaService.setInfoText("Imprimante Evolis non prête", "alert-danger");
                        }
                    }
                    Utils.sleep(2000);
                }
            }
        };
    }
}
