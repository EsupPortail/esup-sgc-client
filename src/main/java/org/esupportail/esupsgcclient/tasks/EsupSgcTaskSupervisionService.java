package org.esupportail.esupsgcclient.tasks;

import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcRestClientService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;
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
