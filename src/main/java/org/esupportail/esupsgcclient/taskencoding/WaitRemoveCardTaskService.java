package org.esupportail.esupsgcclient.taskencoding;

import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Task;

public class WaitRemoveCardTaskService extends EsupSgcTaskService<Void> {

	static long lastRunTime = 100000;

	protected Task<Void> createTask() {
		Task<Void> waitRemoveCardTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateTitle("Merci de retirer cette carte");
				while (!EncodingService.waitForCardAbsent(1000)) {
					// Utils.sleep(1000); -  sleep non nécessaire : EncodingService.waitForCardAbsent l'intègre
				}
				updateTitle("Carte retirée");
				return null;
			}
		};
		return waitRemoveCardTask;
	}

	@Override
	public EsupSgcTaskService getNext() {
		return null;
	}
}
