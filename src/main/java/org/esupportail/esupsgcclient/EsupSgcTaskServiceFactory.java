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
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.taskencoding.EsupSgcLongPollTaskService;
import org.esupportail.esupsgcclient.taskencoding.EsupSgcTaskService;
import org.esupportail.esupsgcclient.taskencoding.QrCodeTaskService;
import org.esupportail.esupsgcclient.taskencoding.TaskParamBean;
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

    EsupSgcLongPollTaskService evolisEsupSgcLongPollTaskService;

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
            uiSteps.get(step).getStyleClass().add("alert-warning");
        }
    }

    /*
     must be run from App JFX Thread
     */
    public void runQrCodeTaskService() {
        if(qrCodeTaskService!=null && qrCodeTaskService.isRunning()) {
            qrCodeTaskService.cancel();
        }
        qrCodeTaskService = new QrCodeTaskService(new TaskParamBean(uiSteps, TaskParamBean.RootType.qrcode, null, webcamImageView.imageProperty(), null,
                null, bmpColorImageView, bmpBlackImageView,
                null,null,
                true, true));
        setupFlowEsupSgcTaskService(qrCodeTaskService);
    }

    /*
    must be run from App JFX Thread
    */
    public void runEvolisEsupSgcLongPollTaskService() {
        if(evolisEsupSgcLongPollTaskService !=null && evolisEsupSgcLongPollTaskService.isRunning()) {
            evolisEsupSgcLongPollTaskService.cancel();
        }
        evolisEsupSgcLongPollTaskService = new EsupSgcLongPollTaskService(new TaskParamBean(uiSteps, TaskParamBean.RootType.evolis, null, webcamImageView.imageProperty(), null,
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
                esupSgcTaskService.setUiStepSuccess();
                progressBar.setStyle("");
                setupFlowEsupSgcTaskService(esupSgcTaskService.getNextWhenSuccess());
            }
        });

        esupSgcTaskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                log.error("Exception when procressing card ...", esupSgcTaskService.getException());
                esupSgcTaskService.setUiStepFailed(esupSgcTaskService.getException());
                progressBar.setStyle("-fx-accent:red");
                setupFlowEsupSgcTaskService(esupSgcTaskService.getNextWhenFail());
                if(esupSgcTaskService.getException() != null && esupSgcTaskService.getException().getMessage()!=null) {
                    logTextarea.appendText(esupSgcTaskService.getException().getMessage());
                }
            }
        });

        textPrincipal.textProperty().bind(esupSgcTaskService.titleProperty());
        esupSgcTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));
        progressBar.progressProperty().bind(esupSgcTaskService.progressProperty());
        log.debug("restart " + esupSgcTaskService);
        if(esupSgcTaskService.isRoot()) {
            resetUiSteps();
        }
        esupSgcTaskService.restart();
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
