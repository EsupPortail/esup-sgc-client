package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterCommands;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.ui.UiStep;

public class EvolisPrintTaskService extends EsupSgcTaskService<Void> {

	private final static Logger log = Logger.getLogger(EvolisPrintTaskService.class);

	public EvolisPrintTaskService(TaskParamBean taskParamBean) {
		super(taskParamBean);
	}

	@Override
	protected Task<Void> createTask() {
		Task<Void> evolisTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateProgress(1,10);
				updateTitle("Insertion de la carte");
				EvolisPrinterService.insertCard();
				setUiStepSuccess(UiStep.printer_insert);
				updateProgress(2,10);
				updateTitle("Panneau couleur");
				EvolisPrinterService.printFrontColorBmp(taskParamBean.bmpColorAsBase64);
				setUiStepSuccess(UiStep.printer_color);
				updateProgress(3,10);
				updateTitle("Panneau noir");
				EvolisPrinterService.printFrontBlackBmp(taskParamBean.bmpBlackAsBase64);
				setUiStepSuccess(UiStep.printer_black);
				updateProgress(4,10);
				updateTitle("Overlay");
				EvolisPrinterService.printFrontVarnish("todo");
				setUiStepSuccess(UiStep.printer_overlay);
				updateProgress(5,10);
				updateTitle("Impression...");
				EvolisPrinterService.print();
				setUiStepSuccess(UiStep.printer_print);
				updateProgress(10,10);
				updateTitle("Positionnement sur lecteur NFC");
				EvolisPrinterService.insertCardToContactLessStation();
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
