package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Service;
import org.esupportail.esupsgcclient.ui.UiStep;

public abstract class EsupSgcTaskService<S> extends Service<S> {

	final TaskParamBean taskParamBean;

	public EsupSgcTaskService(TaskParamBean taskParamBean) {
		this.taskParamBean = taskParamBean;
	}

	public boolean isRoot() {
		return false;
	}

	public abstract EsupSgcTaskService getNextWhenSuccess();

	public EsupSgcTaskService getNextWhenFail() {
		if(TaskParamBean.RootType.qrcode.equals(taskParamBean.rootType)) {
			return  new WaitRemoveCardTaskService(taskParamBean);
		} else if(TaskParamBean.RootType.evolis.equals(taskParamBean.rootType)) {
			return  new EvolisEjectTaskService(new TaskParamBean(taskParamBean.uiSteps, taskParamBean.rootType, taskParamBean.qrcode, taskParamBean.webcamImageProperty, taskParamBean.csn,
					taskParamBean.bmpType, taskParamBean.bmpColorImageView, taskParamBean.bmpBlackImageView,
					taskParamBean.bmpColorAsBase64, taskParamBean.bmpBlackAsBase64,
					false, taskParamBean.fromPrinter));
		}
		throw new RuntimeException("taskParamBean.rootType is null ? - " + taskParamBean.rootType);
	}

	UiStep getUiStep() {
		return null;
	}

	public void setUiStepSuccess() {
		if(getUiStep() !=null) {
			setUiStepSuccess(getUiStep());
		}
	}
	public void setUiStepRunning() {
		if(getUiStep() !=null) {
			taskParamBean.uiSteps.get(getUiStep()).getStyleClass().clear();
			taskParamBean.uiSteps.get(getUiStep()).getStyleClass().add("alert-warning");
		}
	}

	public void setUiStepSuccess(UiStep uiStep) {
		taskParamBean.uiSteps.get(uiStep).getStyleClass().clear();
		taskParamBean.uiSteps.get(uiStep).getStyleClass().add("alert-success");
	}

	public void setUiStepFailed(Throwable exception) {
		if(getUiStep() !=null) {
			setUiStepFailed(getUiStep(), exception);
		}
	}

	public void setUiStepFailed(UiStep uiStep, Throwable exception) {
		taskParamBean.uiSteps.get(uiStep).getStyleClass().clear();
		taskParamBean.uiSteps.get(uiStep).getStyleClass().add("alert-danger");
	}


}
