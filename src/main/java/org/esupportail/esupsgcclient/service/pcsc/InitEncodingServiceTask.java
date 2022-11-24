package org.esupportail.esupsgcclient.service.pcsc;

import javafx.application.Platform;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSgcClientApplication;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.EsupSgcClientJfxController;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Task;

/*
TODO : Refactor as javafx.concurrent.Service
 */
public class InitEncodingServiceTask extends Task<Void> {

    final static Logger log = Logger.getLogger(InitEncodingServiceTask.class);


    private EsupSgcClientJfxController mainPane;

    public InitEncodingServiceTask(EsupSgcClientJfxController mainPane) {
        this.mainPane = mainPane;
    }

    @Override
    protected Void call() {

        try {
            EncodingService.init();
            mainPane.logTextarea.appendText("pc/sc : OK\n");


            /* TODO ...
            if (EncodingService.isEncodeCnous()) {
                mainPane.logTextarea.appendText("dll cnous : OK");
            } else {
                mainPane.hideCnousSteps();
            }*/

            mainPane.logTextarea.appendText("numeroId = " + EsupSgcClientApplication.numeroId);
            mainPane.logTextarea.appendText("sgcAuthToken = " + EsupSgcClientApplication.sgcAuthToken);
            mainPane.logTextarea.appendText( "esupNfcTagServerUrl = " + EncodingService.getEsupNfcTagServerUrl());
            mainPane.logTextarea.appendText( "sgcUrl = " + EncodingService.getSgcUrl());
            mainPane.nfcReady.setValue(true);

        } catch (CnousFournisseurCarteException e) {
            log.error("Erreur de configuration cnous", e);
        } catch (EncodingException | PcscException e) {
            log.error("Erreur lecteur PC/SC", e);
        }
        return null;
    }


    private void customLog(String level, String message, Throwable throwable) {
        Platform.runLater(new Runnable() {
            @Override
            public void run() {
                if ("ERROR".equals(level)) {
                    if (throwable != null) {
                        log.error(message, throwable);
                        mainPane.addLogTextLn(level, throwable.getMessage());
                        mainPane.addLogTextLn(level, Utils.getExceptionString(throwable));
                    } else {
                        log.error(message);
                        mainPane.addLogTextLn(level, message);
                    }
                    mainPane.changeTextPrincipal(message, EsupSgcClientJfxController.StyleLevel.danger);
                    mainPane.changeStepClientReady("Client non prêt", EsupSgcClientJfxController.StyleLevel.danger);
                } else if ("WARN".equals(level)) {
                    if (throwable != null) {
                        log.warn(message, throwable);
                    } else {
                        log.warn(message, throwable);
                    }
                    mainPane.changeTextPrincipal(message, EsupSgcClientJfxController.StyleLevel.warning);
                    mainPane.addLogTextLn(level, message);
                }
                Utils.playSound("fail.wav");
            }
        });
    }

}
