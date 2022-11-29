package org.esupportail.esupsgcclient;

import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.tasks.EvolisTaskService;
import org.esupportail.esupsgcclient.tasks.QrCodeTaskService;
import org.esupportail.esupsgcclient.tasks.TaskParamBean;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;

/*
    Permet de créer les chaines (boulces) de EsupSgcTaskService
 */
public class EsupSgcTaskServiceFactory {

    final static Logger log = Logger.getLogger(EsupSgcTaskServiceFactory.class);

    final FlowPane actionsPane;

    final ImageView webcamImageView;

    final ImageView bmpColorImageView;

    final ImageView bmpBlackImageView;

    final TextArea logTextarea;

    final ProgressBar progressBar;

    final Label textPrincipal;

    QrCodeTaskService qrCodeTaskService;

    EvolisTaskService evolisEvolisTaskService;

    Map<UiStep, TextFlow> uiSteps = new HashMap<>();

    public EsupSgcTaskServiceFactory(ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView,
                                     TextArea logTextarea, ProgressBar progressBar, Label textPrincipal,
                                     FlowPane actionsPane) {
        this.actionsPane = actionsPane;
        this.webcamImageView = webcamImageView;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
        this.logTextarea = logTextarea;
        this.progressBar = progressBar;
        this.textPrincipal = textPrincipal;

        for(UiStep step : UiStep.values()) {
            TextFlow textFlow = getTaskUiTemplate();
            textFlow.managedProperty().bind(textFlow.visibleProperty());
            Label label = (Label)textFlow.getChildren().get(0);
            label.setText(step.toString());
            actionsPane.getChildren().add(actionsPane.getChildren().size(), textFlow);
            uiSteps.put(step, textFlow);
        }
        resetUiSteps();
    }

    private void resetUiSteps() {
        for(UiStep step : uiSteps.keySet()) {
            uiSteps.get(step).setVisible(false);
            uiSteps.get(step).getStyleClass().clear();
            uiSteps.get(step).getStyleClass().add("alert-info");
        }
    }

    /*
     must be run from App JFX Thread
     */
    public void runQrCodeTaskService() {
        if(qrCodeTaskService!=null && qrCodeTaskService.isRunning()) {
            qrCodeTaskService.cancel();
        }
        qrCodeTaskService = new QrCodeTaskService(new TaskParamBean(uiSteps,  webcamImageView.imageProperty(), bmpColorImageView, bmpBlackImageView));
        // TODO setupFlowEsupSgcTaskService(qrCodeTaskService);
    }

    /*
    must be run from App JFX Thread
    */
    public void runEvolisEsupSgcLongPollTaskService() {
        if(evolisEvolisTaskService !=null && evolisEvolisTaskService.isRunning()) {
            evolisEvolisTaskService.cancel();
        }
        evolisEvolisTaskService = new EvolisTaskService(new TaskParamBean(uiSteps,  webcamImageView.imageProperty(), bmpColorImageView, bmpBlackImageView));
        evolisEvolisTaskService.setOnRunning(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                evolisEvolisTaskService.setUiStepRunning();
            }
        });
        evolisEvolisTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                progressBar.setStyle("");
                evolisEvolisTaskService.restart();
            }
        });

        evolisEvolisTaskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                log.error("Exception when procressing card ...", evolisEvolisTaskService.getException());
                evolisEvolisTaskService.setUiStepFailed(UiStep.printer_print, evolisEvolisTaskService.getException());
                progressBar.setStyle("-fx-accent:red");
                if(evolisEvolisTaskService.getException() != null && evolisEvolisTaskService.getException().getMessage()!=null) {
                    logTextarea.appendText(evolisEvolisTaskService.getException().getMessage());
                }
            }
        });

        textPrincipal.textProperty().bind(evolisEvolisTaskService.titleProperty());
        evolisEvolisTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));
        progressBar.progressProperty().bind(evolisEvolisTaskService.progressProperty());
        resetUiSteps();
        evolisEvolisTaskService.restart();
    }


    public TextFlow getTaskUiTemplate() {
        URL fxmlUrl = this.getClass().getClassLoader().getResource("esup-sgc-client-action-template.fxml");
        FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
        try {
            return fxmlLoader.load();
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }

}
