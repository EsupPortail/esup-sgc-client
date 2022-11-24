package org.esupportail.esupsgcclient.taskencoding;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Task;

public class WaitRemoveCardTaskService extends EsupSgcTaskService<Void> {

	final static Logger log = Logger.getLogger(WaitRemoveCardTaskService.class);

	public WaitRemoveCardTaskService(TaskParamBean taskParamBean) {
		super(taskParamBean);
	}

	protected Task<Void> createTask() {
		Task<Void> waitRemoveCardTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				try {
					updateTitle("Merci de retirer cette carte");
					while (!EncodingService.waitForCardAbsent(1000)) {
						// Utils.sleep(1000); -  sleep non nécessaire : EncodingService.waitForCardAbsent l'intègre
					}
					updateTitle("Carte retirée");
				} catch(Exception e) {
					// tâche de fin - on ne tolère pas d'erreur ici
					updateTitle("Erreur NFC ? " + e.getMessage());
					log.error("Erreur NFC ?", e);
				}
				return null;
			}
		};
		return waitRemoveCardTask;
	}

	@Override
	public EsupSgcTaskService getNextWhenSuccess() {
		return new QrCodeTaskService(taskParamBean);
	}

	@Override
	public EsupSgcTaskService getNextWhenFail() {
		return getNextWhenSuccess();
	}

}
