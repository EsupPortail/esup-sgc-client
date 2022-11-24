package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;

public class EvolisEjectTaskService extends EsupSgcTaskService<Void> {

	final static Logger log = Logger.getLogger(EvolisEjectTaskService.class);

	public EvolisEjectTaskService(TaskParamBean taskParamBean) {
		super(taskParamBean);
	}

	@Override
	protected Task<Void> createTask() {
		Task<Void> evolisTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				try {
					updateProgress(1, 2);
					updateTitle("Ejection de la carte");
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
	public EsupSgcTaskService getNext() {
		return null;
	}
}
