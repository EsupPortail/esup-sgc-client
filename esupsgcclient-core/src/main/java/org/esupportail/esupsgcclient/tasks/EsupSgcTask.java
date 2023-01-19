package org.esupportail.esupsgcclient.tasks;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;

import java.util.List;
import java.util.Map;

public abstract class EsupSgcTask extends Task<String> {

    private final static Logger log = Logger.getLogger(EsupSgcTask.class);

    private final static int PROGRESS_STEP_LENGTH = 200;

    Map<UiStep, TextFlow> uiSteps;

    UiStep lastUiStepSuccess = null;

    public EsupSgcTask(Map<UiStep, TextFlow> uiSteps) {
        this.uiSteps = uiSteps;
    }

    protected abstract List<UiStep> getUiStepsList();

    protected void setUiStepFailed(UiStep uiStep, Throwable exception) {
        uiSteps.get(uiStep).getStyleClass().clear();
        uiSteps.get(uiStep).getStyleClass().add("alert-danger");
    }

    protected void setUiStepRunning() {
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
        lastUiStepSuccess = null;
    }

   public void  updateTitle4thisTask(String title) {
        updateTitle(title);
        // just to let time to update title / logtextarea in UI
        Utils.sleep(5);
    }

	protected void setUiStepSuccess(UiStep uiStep) {
        if(uiStep == null) {
            updateProgress(0, getUiStepsList().size()*PROGRESS_STEP_LENGTH);
            updateTitle("En attente ...");
        } else {
            TextFlow uiStepTextFlow = (TextFlow) uiSteps.get(uiStep);
            uiStepTextFlow.getStyleClass().clear();
            uiStepTextFlow.getStyleClass().add("alert-success");
            updateProgress((getUiStepsList().indexOf(uiStep)+1)*PROGRESS_STEP_LENGTH, getUiStepsList().size()*PROGRESS_STEP_LENGTH);
            if(getUiStepsList().indexOf(uiStep)+1<getUiStepsList().size()) {
                UiStep newtUiStep = (UiStep) getUiStepsList().get(getUiStepsList().indexOf(uiStep) + 1);
                updateTitle(newtUiStep.toString());
            }
        }
        lastUiStepSuccess = uiStep;
	}

    public void updateProgressStep() {
       Platform.runLater(() -> {
           updateProgress(getProgress() * getUiStepsList().size() * PROGRESS_STEP_LENGTH + 1, getUiStepsList().size() * PROGRESS_STEP_LENGTH);
       });
    }

    protected void setCurrentUiStepFailed(Throwable exception) {
        UiStep currentUiStep = null;
        if(lastUiStepSuccess==null) {
            currentUiStep = getUiStepsList().get(0);
        } else if(lastUiStepSuccess!=null && getUiStepsList().indexOf(lastUiStepSuccess)+1<getUiStepsList().size()) {
            currentUiStep = (UiStep) getUiStepsList().get(getUiStepsList().indexOf(lastUiStepSuccess) + 1);
        }
         if(currentUiStep!=null) {
             setUiStepFailed(currentUiStep, exception);
         }
    }

}
