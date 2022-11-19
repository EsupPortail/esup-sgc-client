package org.esupportail.esupsgcclient.task;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;

public class CheckWebcamTask extends Task<Boolean> {

	private final static Logger log = Logger.getLogger(CheckWebcamTask.class);

	
	private SimpleBooleanProperty webCamReady;
	
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
		log.info("webcam is ready");
		return webCamReady.get();
	}

}
