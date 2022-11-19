package org.esupportail.esupsgcclient.task;

import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Task;

public class WaitRemoveCardTask extends Task<Void> {

	@Override
    protected Void call() throws Exception {
		while (!EncodingService.pcscCardOnTerminal()) {
			Utils.sleep(1000);
		}
		return null;
    }

}
