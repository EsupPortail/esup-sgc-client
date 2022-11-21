package org.esupportail.esupsgcclient.task;

import javafx.concurrent.Service;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Task;

public class WaitRemoveCardTaskService extends Service<Void> {

	protected Task<Void> createTask() {
		Task<Void> waitRemoveCardTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				while (!EncodingService.pcscCardOnTerminal()) {
					Utils.sleep(1000);
				}
				return null;
			}
		};
		return waitRemoveCardTask;
	}
}
