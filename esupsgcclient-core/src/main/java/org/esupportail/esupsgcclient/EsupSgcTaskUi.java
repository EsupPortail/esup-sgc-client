package org.esupportail.esupsgcclient;

import javafx.application.Platform;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.Node;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskService;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

public class EsupSgcTaskUi {
    final static Logger log = Logger.getLogger(EsupSgcTaskUi.class);
    EsupSgcTaskService service;
    FlowPane actionsPane;
    ProgressBar progressBar;
    TextArea logTextarea;
    Label textPrincipal;
    Map<UiStep, TextFlow> uiSteps;
    ImageView webcamImageView;
    ImageView bmpColorImageView;
    ImageView bmpBlackImageView;

    public EsupSgcTaskUi(EsupSgcTaskService service, FlowPane actionsPane, ProgressBar progressBar, TextArea logTextarea, Label textPrincipal,
                         Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
        this.service = service;
        this.actionsPane = actionsPane;
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
                runTaskService();
            }
        });

        service.setOnFailed(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                log.error("Exception when procressing card ...", service.getException());
                progressBar.setStyle("-fx-accent:red");
                if(service.getException() != null && service.getException().getMessage()!=null) {
                    logTextarea.appendText(service.getException().getMessage() + "\n");
                }
                textPrincipal.textProperty().unbind();
                textPrincipal.setText("...");
            }
        });

        service.setOnCancelled(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                log.info("Cancel called");
                logTextarea.appendText("Service stoppé\n");
                progressBar.setStyle("-fx-accent:red");
                textPrincipal.textProperty().unbind();
                textPrincipal.setText("...");
            }
        });
        service.titleProperty().addListener((observable, oldValue, newValue) -> {
            if(newValue.length()>1) {
                logTextarea.appendText(newValue + "\n");
                log.info(newValue);
            } else if(newValue.length()==1) {
                // case of simple '.' or '_' from encoding task
                logTextarea.appendText(newValue);
            }
        });
    }


    /*
        must be run from App JFX Thread
    */
    public void runTaskService() {
        service.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView);
        service.restart();
        progressBar.setStyle("");
        progressBar.progressProperty().bind(service.progressProperty());
        textPrincipal.textProperty().bind(Bindings.format("%.60s", service.titleProperty()));
        for(UiStep uiStep : service.getUiStepsList()) {
            actionsPane.getChildren().stream().filter(node -> ((Label) ((TextFlow) node).getChildren().get(0)).getText().equals(uiStep.toString())).findFirst().get().toFront();
        }
    }

    public BooleanBinding readyToRunProperty() {
        return service.readyToRunProperty();
    }

    public String readyToRunPropertyDisplayProblem() {
        return service.readyToRunPropertyDisplayProblem();
    }

    public void cancelTaskService() {
        Platform.runLater(() -> {
                this.service.cancel();
        });
    }
}
