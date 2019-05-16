package org.esupportail.esupsgcclient.task;

import org.esupportail.esupsgcclient.utils.Utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;


public class WaitClientReadyTask extends Task<Void> {

	public BooleanProperty clientReady = new SimpleBooleanProperty(false);
	
	@Override
    protected Void call() throws Exception {
		while (!clientReady.getValue()) {
			Utils.sleep(1000);
		}
		return null;
    }

}
