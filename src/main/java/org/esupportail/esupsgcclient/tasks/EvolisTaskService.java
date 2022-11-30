package org.esupportail.esupsgcclient.tasks;

import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcLongPollService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Service
public class EvolisTaskService extends javafx.concurrent.Service<String> {

	private final static Logger log = Logger.getLogger(EvolisTaskService.class);

	Map<UiStep, TextFlow> uiSteps;
	ImageView bmpColorImageView;

	ImageView bmpBlackImageView;

	@Resource
	EsupSgcLongPollService esupSgcLongPollService;

	@Resource
	EncodingService encodingService;

	@Resource
	EvolisPrinterService evolisPrinterService;

	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		this.uiSteps = uiSteps;
		this.bmpColorImageView = bmpColorImageView;
		this.bmpBlackImageView = bmpBlackImageView;
	}

	@Override
	protected Task<String> createTask() {
		return new EvolisTask(uiSteps, bmpColorImageView, bmpBlackImageView, esupSgcLongPollService, evolisPrinterService, encodingService);
	}


}
