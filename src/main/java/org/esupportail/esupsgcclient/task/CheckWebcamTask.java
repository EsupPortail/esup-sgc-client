package org.esupportail.esupsgcclient.task;

import org.esupportail.esupsgcclient.utils.Utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;

@SuppressWarnings("restriction")
public class CheckWebcamTask extends Task<Boolean> {

	private SimpleBooleanProperty webCamReady = new SimpleBooleanProperty(false);
	
	public CheckWebcamTask(SimpleBooleanProperty webCamReady) {
		super();
		this.webCamReady = webCamReady;
	}

	@Override
	protected Boolean call() throws Exception {
		int webcamRetryCount = 0;
		while (!webCamReady.get()) {
			if (webcamRetryCount > 10)
				break;
			webcamRetryCount++;
			Utils.sleep(1000);
		}
		return webCamReady.get();
	}

}
