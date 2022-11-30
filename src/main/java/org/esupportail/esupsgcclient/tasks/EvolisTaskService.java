package org.esupportail.esupsgcclient.tasks;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisException;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisResponse;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcLongPollService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.Map;

@Service
public class EvolisTaskService extends EsupSgcTaskService<String> {

	private final static Logger log = Logger.getLogger(EvolisTaskService.class);

	ImageView bmpColorImageView;

	ImageView bmpBlackImageView;


	@Resource
	EsupSgcLongPollService esupSgcLongPollService;

	@Resource
	EncodingService encodingService;

	@Resource
	EvolisPrinterService evolisPrinterService;

	public void init(Map<UiStep, TextFlow> uiSteps, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super.init(uiSteps);
		this.bmpColorImageView = bmpColorImageView;
		this.bmpBlackImageView = bmpBlackImageView;
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
					String bmpBlackAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.black);
					updateBmpUi(bmpBlackAsBase64, bmpBlackImageView);
					setUiStepSuccess(UiStep.bmp_black);
					String bmpColorAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.color);
					updateBmpUi(bmpColorAsBase64, bmpColorImageView);
					setUiStepSuccess(UiStep.bmp_color);
					updateProgress(1, 10);
					EvolisResponse resp = evolisPrinterService.insertCardToContactLessStation();
					setUiStepSuccess(UiStep.printer_nfc);
					encodingService.encode(qrcode);
					setUiStepSuccess(UiStep.encode);
					updateProgress(1, 10);
					updateTitle("Impression ...");
					evolisPrinterService.printBegin();
					evolisPrinterService.printSet();
					updateProgress(2, 10);
					updateTitle("Panneau couleur");
					evolisPrinterService.printFrontColorBmp(bmpColorAsBase64);
					setUiStepSuccess(UiStep.printer_color);
					updateProgress(3, 10);
					updateTitle("Panneau noir");
					evolisPrinterService.printFrontBlackBmp(bmpBlackAsBase64);
					setUiStepSuccess(UiStep.printer_black);
					updateProgress(4, 10);
					updateTitle("Overlay");
					evolisPrinterService.printFrontVarnish(bmpBlackAsBase64);
					setUiStepSuccess(UiStep.printer_overlay);
					updateProgress(5, 10);
					updateTitle("Impression...");
					evolisPrinterService.print();
					setUiStepSuccess(UiStep.printer_print);
					updateProgress(10, 10);
				} catch(EvolisException evolisException) {
					updateTitle(evolisException.getMessage());
					log.error("Exception with evolis : " + evolisException.getMessage(), evolisException);
				} finally {
					evolisPrinterService.printEnd();
				}
				return null;
			}
		};
		return esupSgcLongPollTask;
	}

	private void updateBmpUi(String bmpAsBase64, ImageView bmpImageView) {
		try {
			byte[] bmp = Base64.getDecoder().decode(bmpAsBase64.getBytes());
			BufferedImage input_image = ImageIO.read(new ByteArrayInputStream(bmp));
			ByteArrayOutputStream out = new ByteArrayOutputStream();
			ImageIO.write(input_image, "PNG", out);
			bmpImageView.setImage(new Image(new ByteArrayInputStream(out.toByteArray()), 200, 200, true, true));
		}catch(Exception e) {
			log.warn("pb refreshing bmpImageView with bmpAsBase64", e);
		}
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
