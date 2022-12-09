package org.esupportail.esupsgcclient;

import javafx.application.Platform;
import javafx.beans.binding.BooleanBinding;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Label;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.text.TextFlow;
import org.apache.http.impl.client.HttpClients;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskSupervisionService;
import org.esupportail.esupsgcclient.tasks.EvolisReadNfcTaskService;
import org.esupportail.esupsgcclient.tasks.EvolisTaskService;
import org.esupportail.esupsgcclient.tasks.QrCodeTaskService;
import org.esupportail.esupsgcclient.tasks.ReadNfcTaskService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ThreadPoolExecutor;


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

    @Resource
    EvolisReadNfcTaskService evolisReadNfcTaskService;

    @Resource
    EsupSgcTaskSupervisionService esupSgcTaskSupervisionService;

    @Resource
    ReadNfcTaskService readNfcTaskService;

    @Resource
    HttpComponentsClientHttpRequestFactory httpRequestFactory;

    @Resource
    ThreadPoolExecutor sgcTaskExecutor;

    Map<UiStep, TextFlow> uiSteps = new HashMap<>();

    Map<String, EsupSgcTaskUi> esupSgcTaskUis = new HashMap<>();

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

        esupSgcTaskSupervisionService.start();

        qrCodeTaskService.setExecutor(sgcTaskExecutor);
        evolisTaskService.setExecutor(sgcTaskExecutor);
        evolisReadNfcTaskService.setExecutor(sgcTaskExecutor);
        readNfcTaskService.setExecutor(sgcTaskExecutor);

        esupSgcTaskUis.put("Encodage par scan de QRCode", new EsupSgcTaskUi(qrCodeTaskService, progressBar, logTextarea, textPrincipal, uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView));
        esupSgcTaskUis.put("Encodage et impression via Evolis Primacy", new EsupSgcTaskUi(evolisTaskService, progressBar, logTextarea, textPrincipal, uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView));
        esupSgcTaskUis.put("Badgeage via Evolis Primacy", new EsupSgcTaskUi(evolisReadNfcTaskService, progressBar, logTextarea, textPrincipal, uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView));
        esupSgcTaskUis.put("Badgeage simple", new EsupSgcTaskUi(readNfcTaskService, progressBar, logTextarea, textPrincipal, uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView));

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
    }

    public void runService(String newServiceName) {
        Platform.runLater(() -> {
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
}
