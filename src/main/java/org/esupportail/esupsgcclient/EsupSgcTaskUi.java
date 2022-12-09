package org.esupportail.esupsgcclient;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskService;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Map;


public class EsupSgcTaskUi {
    final static Logger log = Logger.getLogger(EsupSgcTaskUi.class);
    EsupSgcTaskService service;
    ProgressBar progressBar;
    TextArea logTextarea;
    Label textPrincipal;
    Map<UiStep, TextFlow> uiSteps;
    ImageView webcamImageView;
    ImageView bmpColorImageView;
    ImageView bmpBlackImageView;

    public EsupSgcTaskUi(EsupSgcTaskService service, ProgressBar progressBar, TextArea logTextarea, Label textPrincipal,
                         Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
        this.service = service;
        this.progressBar = progressBar;
        this.logTextarea = logTextarea;
        this.textPrincipal = textPrincipal;
        this.uiSteps = uiSteps;
        this.webcamImageView = webcamImageView;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;

        service.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                restartTaskService();
            }
        });

        service.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                log.error("Exception when procressing card ...", service.getException());
                progressBar.setStyle("-fx-accent:red");;
                if(service.getException() != null && service.getException().getMessage()!=null) {
                    logTextarea.appendText(service.getException().getMessage() + "\n");
                }
            }
        });

        textPrincipal.textProperty().bind(service.titleProperty());
        service.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));
    }

    private void restartTaskService() {
        service.restart();
    }


    /*
        must be run from App JFX Thread
    */
    public void runTaskService() {
        service.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView);
        progressBar.setStyle("");
        progressBar.progressProperty().bind(service.progressProperty());
        service.restart();
    }

    public boolean isReadyToRun(AppSession appSession) {
        return service.isReadyToRun(appSession);
    }

    public void cancelTaskService() {
        this.service.cancel();
    }
}
