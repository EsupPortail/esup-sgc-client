package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Service;

public abstract class EsupSgcTaskService<S> extends Service<S> {

	final TaskParamBean taskParamBean;

	public EsupSgcTaskService(TaskParamBean taskParamBean) {
		this.taskParamBean = taskParamBean;
	}

	public abstract EsupSgcTaskService getNextWhenSuccess();

	public EsupSgcTaskService getNextWhenFail() {
		if(TaskParamBean.RootType.qrcode.equals(taskParamBean.rootType)) {
			return  new WaitRemoveCardTaskService(taskParamBean);
		} else if(TaskParamBean.RootType.evolis.equals(taskParamBean.rootType)) {
			return  new EvolisEjectTaskService(new TaskParamBean(taskParamBean.rootType, taskParamBean.qrcode, taskParamBean.webcamImageProperty, taskParamBean.csn,
					taskParamBean.bmpType, taskParamBean.bmpColorImageView, taskParamBean.bmpBlackImageView,
					taskParamBean.bmpColorAsBase64, taskParamBean.bmpBlackAsBase64,
					false, taskParamBean.fromPrinter));
		}
		throw new RuntimeException("taskParamBean.rootType is null ? - " + taskParamBean.rootType);
	}
}
