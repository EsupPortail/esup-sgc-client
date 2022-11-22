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
				while (!EncodingService.pcscCardOnTerminal()) {
					updateTitle("Merci de retirer cette carte");
					Utils.sleep(1000);
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
