package org.esupportail.esupsgcclient.task;

import org.esupportail.esupsgcclient.service.EncodingService;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Task;

@SuppressWarnings("restriction")
public class WaitRemoveCardTask extends Task<Void> {

	@Override
    protected Void call() throws Exception {
		while (!EncodingService.pcscCardOnTerminal()) {
			Utils.sleep(1000);
		}
		return null;
    }

}
