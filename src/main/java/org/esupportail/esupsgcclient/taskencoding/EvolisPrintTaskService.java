package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterCommands;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.nio.file.Files;
import java.nio.file.Path;

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
				updateTitle("Impression ...");
				EvolisPrinterService.printBegin();
				EvolisPrinterService.printSet();
				setUiStepSuccess(UiStep.printer_insert);
				updateProgress(2,10);
				updateTitle("Panneau couleur");
				//Path filePath = Path.of("/opt/esup-sgc-client/src/etc/black.bmp.txt");
				//String bmp64 = Files.readString(filePath);

				EvolisPrinterService.printFrontColorBmp(taskParamBean.bmpColorAsBase64);
				setUiStepSuccess(UiStep.printer_color);
				updateProgress(3,10);
				updateTitle("Panneau noir");
				EvolisPrinterService.printFrontBlackBmp(taskParamBean.bmpBlackAsBase64);
				setUiStepSuccess(UiStep.printer_black);
				updateProgress(4,10);
				updateTitle("Overlay");
				EvolisPrinterService.printFrontVarnish(taskParamBean.bmpBlackAsBase64);
				setUiStepSuccess(UiStep.printer_overlay);
				updateProgress(5,10);
				updateTitle("Impression...");
				EvolisPrinterService.print();
				//EvolisPrinterService.printEnd();
				setUiStepSuccess(UiStep.printer_print);
				updateProgress(10,10);
				return null;
			}
		};
		return evolisTask;
	}

	@Override
	public EsupSgcTaskService getNextWhenSuccess() {
		return new EvolisEjectTaskService(taskParamBean);
	}

}
