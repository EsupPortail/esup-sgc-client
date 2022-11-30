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
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Service
public class EvolisTaskService extends EsupSgcTaskService<String> {

	private final static Logger log = Logger.getLogger(EvolisTaskService.class);

	static List<UiStep> uiStepsList = Arrays.asList(new UiStep[]{
				UiStep.long_poll,
				UiStep.bmp_black,
				UiStep.bmp_color,
				UiStep.printer_nfc,
				UiStep.encode,
				UiStep.printer_color,
				UiStep.printer_black,
				UiStep.printer_overlay,
				UiStep.encode,
				UiStep.printer_print});

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
		return new EvolisTask(this);
	}


	public void setUiStepRunning() {
		for(UiStep step : uiStepsList) {
			uiSteps.get(step).setVisible(true);
		}
	}


}
