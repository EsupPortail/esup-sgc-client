package org.esupportail.esupsgcclient.tasks;

import javafx.beans.binding.BooleanBinding;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcRestClientService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class ReadNfcTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(ReadNfcTaskService.class);

	@Resource
	EncodingService encodingService;

	@Resource
	AppSession appSession;

	@Override
	protected Task<String> createTask() {
		return new ReadNfcTask(uiSteps, encodingService);
	}


	@Override
	public BooleanBinding readyToRunProperty() {
		return appSession.authReadyProperty().and(appSession.nfcReadyProperty());
	}

	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView);
		webcamImageView.setVisible(false);
		bmpColorImageView.setVisible(false);
		bmpBlackImageView.setVisible(false);
	}
}
