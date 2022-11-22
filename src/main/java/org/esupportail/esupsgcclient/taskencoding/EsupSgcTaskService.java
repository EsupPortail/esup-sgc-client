package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Label;

public abstract class EsupSgcTaskService<S> extends Service<S> {
	public abstract EsupSgcTaskService getNext();

}
