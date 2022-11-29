package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.ui.UiStep;

public class EvolisEjectTaskService extends EsupSgcTaskService<Void> {

	final static Logger log = Logger.getLogger(EvolisEjectTaskService.class);

	public EvolisEjectTaskService(TaskParamBean taskParamBean) {
		super(taskParamBean);
	}

	UiStep getUiStep() {
		return UiStep.printer_eject;
	}

	@Override
	protected Task<Void> createTask() {
		Task<Void> evolisTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				try {
					updateProgress(1, 2);
					updateTitle("Ejection de la carte");
					try {
						EvolisPrinterService.printEnd();
					}catch(Exception e) {
						log.warn("print end exception", e);
					}
					if(taskParamBean.eject4success) {
						EvolisPrinterService.eject();
					} else {
						EvolisPrinterService.reject();
					}
					updateProgress(2, 2);
				} catch(Exception e) {
					// tâche de fin - on ne tolère pas d'erreur ici
					updateTitle("Erreur lors de l'ejection de la carte - " + e.getMessage());
					log.error("Erreur lors de l'ejection de la carte", e);
				}
				return null;
			}
		};
		return evolisTask;
	}

	@Override
	public EsupSgcTaskService getNextWhenSuccess() {
		return new EsupSgcLongPollTaskService(new TaskParamBean(taskParamBean.uiSteps, taskParamBean.rootType, null, null, null,
				EncodingService.BmpType.black, taskParamBean.bmpColorImageView, taskParamBean.bmpBlackImageView,
				null, null,
				true, true));
	}

	@Override
	public EsupSgcTaskService getNextWhenFail() {
		return getNextWhenSuccess();
	}

}
