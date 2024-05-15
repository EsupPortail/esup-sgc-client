package org.esupportail.esupsgcclient;

import javax.annotation.Resource;

import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ChangeListener;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextFlow;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcHeartbeatService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskSupervisionService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;


@Component
public class EsupSgcTaskServiceFactory {

    final static Logger log = Logger.getLogger(EsupSgcTaskServiceFactory.class);

    FlowPane actionsPane;

    ImageView webcamImageView;

    ImageView bmpColorImageView;

    ImageView bmpBlackImageView;

    ImageView bmpBackImageView;

    TextArea logTextarea;

    ProgressBar progressBar;

    Label textPrincipal;

    CheckMenuItem autostart;

    @Resource
    List<EsupSgcTaskService> esupSgcTaskServices;

    @Resource
    EsupSgcTaskSupervisionService esupSgcTaskSupervisionService;

    @Resource
    HttpComponentsClientHttpRequestFactory httpRequestFactory;

    @Resource
    ThreadPoolExecutor sgcTaskExecutor;

    @Resource
    EsupSgcHeartbeatService esupSgcHeartbeatService;

    @Resource
    AppSession appSession;

    Map<UiStep, TextFlow> uiSteps = new HashMap<>();

    Map<String, EsupSgcTaskUi> esupSgcTaskUis = new HashMap<>();

    Map<String, ChangeListener<? super Boolean>> startStopListeners = new HashMap<>();

    public void init(ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView, ImageView bmpBackImageView,
                     TextArea logTextarea, ProgressBar progressBar, Label textPrincipal,
                     FlowPane actionsPane, CheckMenuItem autostart) {
        this.actionsPane = actionsPane;
        this.webcamImageView = webcamImageView;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
        this.bmpBackImageView = bmpBackImageView;
        this.logTextarea = logTextarea;
        this.progressBar = progressBar;
        this.textPrincipal = textPrincipal;
        this.autostart = autostart;

        List<TextFlow> textFlows2Add = new ArrayList<>();
        for(UiStep step : UiStep.values()) {
            TextFlow textFlow = getTaskUiTemplate();
            textFlow.managedProperty().bind(textFlow.visibleProperty());
            Label label = (Label)textFlow.getChildren().get(0);
            label.setText(step.toString());
            textFlows2Add.add(textFlow);
            uiSteps.put(step, textFlow);
            textFlow.setVisible(false);
        }
        actionsPane.getChildren().addAll(textFlows2Add);

        esupSgcTaskSupervisionService.start();

        for(EsupSgcTaskService esupSgcTaskService : esupSgcTaskServices) {
            // 1 thread for all EsupSgcTasks to be sure to avoid multiple runs in parallels
            esupSgcTaskService.setExecutor(sgcTaskExecutor);
            // create esupSgcTaskUis
            esupSgcTaskUis.put(esupSgcTaskService.getLabel(), new EsupSgcTaskUi(esupSgcTaskService, actionsPane, progressBar, logTextarea, textPrincipal, uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView, bmpBackImageView));
            // add startStopListeners setup by @EsupSfcClientJfxController
            startStopListeners.put(esupSgcTaskService.getLabel(), (ChangeListener<Boolean>) (observable, oldValue, newValue) -> {
                if(newValue) {
                    if(autostart.isSelected()) {
                        logTextarea.appendText(String.format("Autostart est activé, le service '%s' va démarrer dans 5 secondes.\n", esupSgcTaskService.getLabel()));
                        Utils.sleep(5000);
                        runService(esupSgcTaskService.getLabel());
                    } else {
                        logTextarea.appendText(String.format("Le service '%s' est prêt à démarrer.\n", esupSgcTaskService.getLabel()));
                    }
                } else {
                    logTextarea.appendText(String.format("Le service '%s' va s'arrêter.\n", esupSgcTaskService.getLabel()));
                    cancelService(esupSgcTaskService.getLabel());
                }
            });
        }

        esupSgcHeartbeatService.setExecutor(Executors.newFixedThreadPool(1));

        esupSgcHeartbeatService.setOnSucceeded(event ->  Utils.jfxRunLaterIfNeeded(() -> {
            // esupSgcHeartbeatService stopped -> esup-sgc restarted ? -> sgcAutoken should be refreshed ? -> iframe on esup-nfc-tag should be refreshed
            appSession.authReadyProperty().set(false);
        }));
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

    public BooleanBinding readyToRunProperty(String serviceName) {
        return esupSgcTaskUis.get(serviceName).readyToRunProperty();
    }

    public String readyToRunPropertyDisplayProblem(String serviceName) {
        return esupSgcTaskUis.get(serviceName).readyToRunPropertyDisplayProblem();
    }

    public void cancelService(String oldServiceName) {
        if(oldServiceName != null) {
            esupSgcTaskUis.get(oldServiceName).cancelTaskService();
        }
        // we destroy all http connections (used by RestTemplate used in all tasks) to help
        try {
            httpRequestFactory.destroy();
        } catch (Exception e) {
            log.debug("Exception destroying httpRequestFactory", e);
        } finally {
            httpRequestFactory.setHttpClient(HttpClients.createSystem());
        }
        resetUiSteps();
    }

    public void runService(String newServiceName) {
        Utils.jfxRunLaterIfNeeded(() -> {
            esupSgcTaskUis.get(newServiceName).runTaskService();
        });
    }

    public List<String> getServicesNames() {
        return new ArrayList<>(esupSgcTaskUis.keySet());
    }

    public void resetUiSteps() {
        for(UiStep step : uiSteps.keySet()) {
            uiSteps.get(step).setVisible(false);
        }
    }

    public ChangeListener<? super Boolean> getStopStartListener(String serviceName) {
        return startStopListeners.get(serviceName);
    }
}
