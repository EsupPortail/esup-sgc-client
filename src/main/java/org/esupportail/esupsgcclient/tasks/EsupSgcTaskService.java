package org.esupportail.esupsgcclient.tasks;

import javafx.concurrent.Service;
import javafx.scene.text.TextFlow;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Map;

public abstract class EsupSgcTaskService<S> extends Service<S> {

	Map<UiStep, TextFlow> uiSteps;

	public EsupSgcTaskService(Map<UiStep, TextFlow> uiSteps) {
		this.uiSteps = uiSteps;
	}

	public void setUiStepRunning(UiStep uiStep) {
		uiSteps.get(uiStep).getStyleClass().clear();
		uiSteps.get(uiStep).getStyleClass().add("alert-warning");
	}

	public void setUiStepSuccess(UiStep uiStep) {
		uiSteps.get(uiStep).getStyleClass().clear();
		uiSteps.get(uiStep).getStyleClass().add("alert-success");
	}

	public void setUiStepFailed(UiStep uiStep, Throwable exception) {
		uiSteps.get(uiStep).getStyleClass().clear();
		uiSteps.get(uiStep).getStyleClass().add("alert-danger");
	}

}
