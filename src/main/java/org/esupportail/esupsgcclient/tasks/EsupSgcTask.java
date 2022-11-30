package org.esupportail.esupsgcclient.tasks;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisException;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisResponse;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.stereotype.Service;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public abstract class EsupSgcTask extends Task<String> {

    private final static Logger log = Logger.getLogger(EvolisTaskService.class);

    Map<UiStep, TextFlow> uiSteps = new HashMap<>();

    public EsupSgcTask(Map<UiStep, TextFlow> uiSteps) {
        this.uiSteps = uiSteps;
    }

    abstract List<UiStep> getUiStepsList();

    public void setup(Map<UiStep, TextFlow> uiSteps) {
        this.uiSteps = uiSteps;
    }

    void setUiStepRunning(UiStep uiStep) {
        uiSteps.get(uiStep).getStyleClass().clear();
        uiSteps.get(uiStep).getStyleClass().add("alert-warning");
    }

    void setUiStepFailed(UiStep uiStep, Throwable exception) {
        uiSteps.get(uiStep).getStyleClass().clear();
        uiSteps.get(uiStep).getStyleClass().add("alert-danger");
    }

    void setUiStepRunning() {
        resetUiSteps();
        for(UiStep step : getUiStepsList()) {
            uiSteps.get(step).setVisible(true);
        }
    }

    void resetUiSteps() {
        for(UiStep step : uiSteps.keySet()) {
            uiSteps.get(step).setVisible(false);
            uiSteps.get(step).getStyleClass().clear();
            uiSteps.get(step).getStyleClass().add("alert-info");
        }
    }

	void setUiStepSuccess(UiStep uiStep) {
        if(uiStep == null) {
            updateProgress(0, getUiStepsList().size());
            updateTitle("En attente ...");
        } else {
            TextFlow uiStepTextFlow = (TextFlow) uiSteps.get(uiStep);
            uiStepTextFlow.getStyleClass().clear();
            uiStepTextFlow.getStyleClass().add("alert-success");
            updateProgress(getUiStepsList().indexOf(uiStep), getUiStepsList().size());
            if(getUiStepsList().indexOf(uiStep)+1<getUiStepsList().size()) {
                UiStep newtUiStep = (UiStep) getUiStepsList().get(getUiStepsList().indexOf(uiStep) + 1);
                updateTitle(newtUiStep.toString());
            }
        }
	}

}
