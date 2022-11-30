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
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


@Component
public class EsupSgcTaskServiceFactory {

    final static Logger log = Logger.getLogger(EsupSgcTaskServiceFactory.class);

    FlowPane actionsPane;

    ImageView webcamImageView;

    ImageView bmpColorImageView;

    ImageView bmpBlackImageView;

    TextArea logTextarea;

    ProgressBar progressBar;

    Label textPrincipal;

    @Resource
    QrCodeTaskService qrCodeTaskService;

    @Resource
    EvolisTaskService evolisTaskService;

    Map<UiStep, TextFlow> uiSteps = new HashMap<>();

    public void init(ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView,
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
            textFlow.setVisible(false);
        }

        initQrCodeTaskService();

        initEvolisTaskService();
    }

     void initQrCodeTaskService() {
        qrCodeTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                progressBar.setStyle("");
                qrCodeTaskService.restart();
            }
        });

        qrCodeTaskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                log.error("Exception when procressing card ...", qrCodeTaskService.getException());
                progressBar.setStyle("-fx-accent:red");
                if(qrCodeTaskService.getException() != null && qrCodeTaskService.getException().getMessage()!=null) {
                    logTextarea.appendText(qrCodeTaskService.getException().getMessage());
                }
            }
        });

        textPrincipal.textProperty().bind(qrCodeTaskService.titleProperty());
        qrCodeTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));
    }

    void initEvolisTaskService() {
        evolisTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                progressBar.setStyle("");
                evolisTaskService.restart();
            }
        });

        evolisTaskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                log.error("Exception when procressing card ...", evolisTaskService.getException());
                // evolisTaskService.setUiStepFailed(UiStep.printer_print, evolisTaskService.getException());
                progressBar.setStyle("-fx-accent:red");
                if(evolisTaskService.getException() != null && evolisTaskService.getException().getMessage()!=null) {
                    logTextarea.appendText(evolisTaskService.getException().getMessage());
                }
            }
        });

        textPrincipal.textProperty().bind(evolisTaskService.titleProperty());
        evolisTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));
    }

    /*
     must be run from App JFX Thread
     */
    public void runQrCodeTaskService() {
        qrCodeTaskService.setup(uiSteps,  webcamImageView.imageProperty());
        progressBar.progressProperty().bind(qrCodeTaskService.progressProperty());
        qrCodeTaskService.restart();
    }

    /*
    must be run from App JFX Thread
    */
    public void runEvolisTaskService() {
        evolisTaskService.setup(uiSteps, bmpColorImageView, bmpBlackImageView);
        progressBar.progressProperty().bind(evolisTaskService.progressProperty());
        evolisTaskService.restart();
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
