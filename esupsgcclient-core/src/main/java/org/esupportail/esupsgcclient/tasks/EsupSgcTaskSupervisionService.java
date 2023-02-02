package org.esupportail.esupsgcclient.tasks;

import javax.annotation.Resource;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Service;

import java.util.concurrent.ThreadPoolExecutor;

@Service
public class EsupSgcTaskSupervisionService extends javafx.concurrent.Service<String> {

	private final static Logger log = Logger.getLogger(EsupSgcTaskSupervisionService.class);

	@Resource
	AppSession appSession;

	@Resource
	ThreadPoolExecutor sgcTaskExecutor;

	@Override
	protected Task<String> createTask() {
		return new Task<String>() {
			@Override
			protected String call() throws Exception {
				while (true) {
					appSession.setTaskIsRunning(sgcTaskExecutor.getActiveCount()!=0);
					Utils.sleep(1000);
				}
			}
		};
	}
}
