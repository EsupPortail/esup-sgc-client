package org.esupportail.esupsgcclient.task;

import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Task;

public class VoidTask extends Task<Void> {

	@Override
    protected Void call() throws Exception {
        Utils.sleep(1000);
        return null;
    }

}
