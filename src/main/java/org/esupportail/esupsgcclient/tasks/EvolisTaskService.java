package org.esupportail.esupsgcclient.tasks;

import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisException;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisResponse;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcLongPollService;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Map;

public class EvolisTaskService extends EsupSgcTaskService<String> {

	private final static Logger log = Logger.getLogger(EvolisTaskService.class);

	EsupSgcLongPollService esupSgcLongPollService;

	ImageView bmpColorImageView;

	ImageView bmpBlackImageView;

	public EvolisTaskService(Map<UiStep, TextFlow> uiSteps, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super(uiSteps);
		this.bmpColorImageView = bmpColorImageView;
		this.bmpBlackImageView = bmpBlackImageView;
		esupSgcLongPollService = new EsupSgcLongPollService();
	}

	@Override
	protected Task<String> createTask() {
		Task<String> esupSgcLongPollTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				try {
					updateTitle("En attente...");
					updateProgress(0, 2);
					String qrcode = esupSgcLongPollService.getQrCode();
					setUiStepSuccess(UiStep.long_poll);
					String bmpBlackAsBase64 = EncodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.black);
					setUiStepSuccess(UiStep.bmp_black);
					String bmpColorAsBase64 = EncodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.color);
					setUiStepSuccess(UiStep.bmp_color);
					updateProgress(1, 10);
					EvolisResponse resp = EvolisPrinterService.insertCardToContactLessStation();
					setUiStepSuccess(UiStep.printer_nfc);
					EncodingService.encode(qrcode);
					setUiStepSuccess(UiStep.encode);
					updateProgress(1, 10);
					updateTitle("Impression ...");
					EvolisPrinterService.printBegin();
					EvolisPrinterService.printSet();
					updateProgress(2, 10);
					updateTitle("Panneau couleur");
					EvolisPrinterService.printFrontColorBmp(bmpColorAsBase64);
					setUiStepSuccess(UiStep.printer_color);
					updateProgress(3, 10);
					updateTitle("Panneau noir");
					EvolisPrinterService.printFrontBlackBmp(bmpBlackAsBase64);
					setUiStepSuccess(UiStep.printer_black);
					updateProgress(4, 10);
					updateTitle("Overlay");
					EvolisPrinterService.printFrontVarnish(bmpBlackAsBase64);
					setUiStepSuccess(UiStep.printer_overlay);
					updateProgress(5, 10);
					updateTitle("Impression...");
					EvolisPrinterService.print();
					setUiStepSuccess(UiStep.printer_print);
					updateProgress(10, 10);
				} catch(EvolisException evolisException) {
					updateTitle(evolisException.getMessage());
					log.error("Exception with evolis : " + evolisException.getMessage(), evolisException);
				} finally {
					EvolisPrinterService.printEnd();
				}
				return null;
			}
		};
		return esupSgcLongPollTask;
	}



	public void setUiStepRunning() {
		for(UiStep step : new UiStep[]{
				UiStep.long_poll,
				UiStep.bmp_black,
				UiStep.bmp_color,
				UiStep.printer_nfc,
				UiStep.encode,
				UiStep.printer_color,
				UiStep.printer_black,
				UiStep.printer_overlay,
				UiStep.encode,
				UiStep.printer_print}) {
			uiSteps.get(step).setVisible(true);
		}
	}


}
