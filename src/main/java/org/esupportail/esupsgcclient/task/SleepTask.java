package org.esupportail.esupsgcclient.task;

import javafx.concurrent.Task;

@SuppressWarnings("restriction")
public class SleepTask extends Task<Void> {

	private long millis;
	
	public SleepTask(long millis) {
		super();
		this.millis = millis;
	}

	@Override
    protected Void call() throws Exception {
        Thread.sleep(millis);
        return null;
    }

}
