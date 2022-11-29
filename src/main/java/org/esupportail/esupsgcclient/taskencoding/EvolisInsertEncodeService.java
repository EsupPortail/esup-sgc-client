package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisResponse;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;

public class EvolisInsertEncodeService extends EsupSgcTaskService<Void> {

	private final static Logger log = Logger.getLogger(EvolisInsertEncodeService.class);

	public EvolisInsertEncodeService(TaskParamBean taskParamBean) {
		super(taskParamBean);
	}

	@Override
	UiStep getUiStep() {
		return UiStep.printer_nfc;
	}

	@Override
	protected Task<Void> createTask() {
		Task<Void> evolisTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateProgress(1,10);
				updateTitle("Positionnement sur lecteur NFC");
				EvolisResponse r = new EvolisResponse();
				String lastMessage = "";
				while(!"OK".equals(lastMessage)) {
					EvolisResponse resp = EvolisPrinterService.insertCardToContactLessStation();
					if(!lastMessage.equals(resp.getResult()) && !"OK".equals(lastMessage)) {
						lastMessage = resp.getResult();
						updateTitle(lastMessage);
						Utils.sleep(5000);
					}
				}
				setUiStepSuccess(UiStep.printer_nfc);
				return null;
			}
		};
		return evolisTask;
	}

	@Override
	public EsupSgcTaskService getNextWhenSuccess() {
		return new EncodingTaskService(taskParamBean);
	}

}
