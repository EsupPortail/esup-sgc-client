package org.esupportail.esupsgcclient.tasks;

import javafx.concurrent.Service;
import org.esupportail.esupsgcclient.ui.UiStep;

public abstract class EsupSgcTaskService<S> extends Service<S> {

	final TaskParamBean taskParamBean;

	public EsupSgcTaskService(TaskParamBean taskParamBean) {
		this.taskParamBean = taskParamBean;
	}

	public void setUiStepRunning(UiStep uiStep) {
		taskParamBean.uiSteps.get(uiStep).getStyleClass().clear();
		taskParamBean.uiSteps.get(uiStep).getStyleClass().add("alert-warning");
	}

	public void setUiStepSuccess(UiStep uiStep) {
		taskParamBean.uiSteps.get(uiStep).getStyleClass().clear();
		taskParamBean.uiSteps.get(uiStep).getStyleClass().add("alert-success");
	}

	public void setUiStepFailed(UiStep uiStep, Throwable exception) {
		taskParamBean.uiSteps.get(uiStep).getStyleClass().clear();
		taskParamBean.uiSteps.get(uiStep).getStyleClass().add("alert-danger");
	}

}
