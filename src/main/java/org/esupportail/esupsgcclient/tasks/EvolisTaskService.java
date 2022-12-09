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
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class EvolisTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(EvolisTaskService.class);

	@Resource
	EsupSgcRestClientService esupSgcRestClientService;

	@Resource
	EncodingService encodingService;

	@Resource
	EvolisPrinterService evolisPrinterService;

	@Resource
	AppSession appSession;

	@Override
	protected Task<String> createTask() {
		return new EvolisTask(uiSteps, bmpColorImageView, bmpBlackImageView, esupSgcRestClientService, evolisPrinterService, encodingService);
	}


	@Override
	public boolean isReadyToRun() {
		return appSession.isAuthReady() && appSession.isNfcReady() && appSession.isPrinterReady() && "DESFIRE".equals(appSession.getAuthType());
	}

	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView);
		webcamImageView.setVisible(false);
		bmpColorImageView.setVisible(true);
		bmpBlackImageView.setVisible(true);
	}
}
