package org.esupportail.esupsgcclient.tasks;

import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.AppSession.READY_CONDITION;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcHeartbeatService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcRestClientService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.Map;

@Service
public class EvolisEncodePrintTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(EvolisEncodePrintTaskService.class);

	static final String ENCODAGE_ET_IMPRESSION_VIA_EVOLIS_PRIMACY = "Encodage et impression via Evolis Primacy";

	@Resource
	EsupSgcRestClientService esupSgcRestClientService;

	@Resource
	EncodingService encodingService;

	@Resource
	EvolisPrinterService evolisPrinterService;

	@Resource
	EsupSgcHeartbeatService esupSgcHeartbeatService;

	@Resource
	AppSession appSession;

	@Override
	protected Task<String> createTask() {
		if(!esupSgcHeartbeatService.isRunning()) {
			esupSgcHeartbeatService.restart();
		}
		return new EvolisEncodePrintTask(uiSteps, bmpColorImageView, bmpBlackImageView, esupSgcRestClientService, evolisPrinterService, encodingService);
	}

	@Override
	public AppSession getAppSession() {
		return appSession;
	}

	@Override
	public String getLabel() {
		return ENCODAGE_ET_IMPRESSION_VIA_EVOLIS_PRIMACY;
	}

	@Override
	public READY_CONDITION[] readyToRunConditions() {
		return new READY_CONDITION[] {READY_CONDITION.auth, READY_CONDITION.nfc,
				READY_CONDITION.printer, READY_CONDITION.nfc_desfire};
	}


	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView);
		webcamImageView.setVisible(false);
		bmpColorImageView.setVisible(true);
		bmpBlackImageView.setVisible(true);
	}
}
