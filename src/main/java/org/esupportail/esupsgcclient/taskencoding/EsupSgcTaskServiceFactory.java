package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSgcClientJfxController;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;

/*
    Permet de créer les chaines (boulces) de EsupSgcTaskService
 */
public class EsupSgcTaskServiceFactory {

    final static Logger log = Logger.getLogger(EsupSgcTaskServiceFactory.class);

    final ImageView webcamImageView;

    final ImageView bmpColorImageView;

    final ImageView bmpBlackImageView;

    final TextArea logTextarea;

    final ProgressBar progressBar;

    final Label textPrincipal;


    QrCodeTaskService qrCodeTaskService;

    EsupSgcLongPollTaskService evolisEsupSgcLongPollTaskService;

    public EsupSgcTaskServiceFactory(ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView, TextArea logTextarea, ProgressBar progressBar, Label textPrincipal) {
        this.webcamImageView = webcamImageView;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
        this.logTextarea = logTextarea;
        this.progressBar = progressBar;
        this.textPrincipal = textPrincipal;
    }

    public void runQrCodeTaskService() {
        if(qrCodeTaskService!=null && qrCodeTaskService.isRunning()) {
            qrCodeTaskService.cancel();
        }
        qrCodeTaskService = new QrCodeTaskService(new TaskParamBean(TaskParamBean.RootType.qrcode, null, webcamImageView.imageProperty(), null,
                null, bmpColorImageView, bmpBlackImageView,
                null,null,
                true, true));
        setupFlowEsupSgcTaskService(qrCodeTaskService);
    }

    public void runEvolisEsupSgcLongPollTaskService() {
        if(evolisEsupSgcLongPollTaskService !=null && evolisEsupSgcLongPollTaskService.isRunning()) {
            evolisEsupSgcLongPollTaskService.cancel();
        }
        evolisEsupSgcLongPollTaskService = new EsupSgcLongPollTaskService(new TaskParamBean(TaskParamBean.RootType.evolis, null, webcamImageView.imageProperty(), null,
                EncodingService.BmpType.black, bmpColorImageView, bmpBlackImageView,
                null,null,
                true, true));
        setupFlowEsupSgcTaskService(evolisEsupSgcLongPollTaskService);
    }

    /*
    Permet de mettre en oeuvre un flow (circulaire) de tâches
    en s'appuyant sur la méthode  EsupSgcTaskService.next()
    qui donne la tâche suivant à effectuer
 */
    void setupFlowEsupSgcTaskService(EsupSgcTaskService esupSgcTaskService) {
        esupSgcTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                setupFlowEsupSgcTaskService(esupSgcTaskService.getNextWhenSuccess());
            }
        });

        esupSgcTaskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                log.error("Exception when procressing card ...", esupSgcTaskService.getException());
                setupFlowEsupSgcTaskService(esupSgcTaskService.getNextWhenFail());
                if(esupSgcTaskService.getException() != null && esupSgcTaskService.getException().getMessage()!=null) {
                    logTextarea.appendText(esupSgcTaskService.getException().getMessage());
                }
            }
        });

        textPrincipal.textProperty().bind(esupSgcTaskService.titleProperty());
        progressBar.progressProperty().bind(esupSgcTaskService.progressProperty());
        log.debug("restart " + esupSgcTaskService);
        esupSgcTaskService.restart();
    }
}
