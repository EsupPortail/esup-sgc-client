package org.esupportail.esupsgcclient.tasks.evolis;

import javax.annotation.Resource;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.AppSession.READY_CONDITION;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisSdkPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcHeartbeatService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class EvolisSdkPrintEncodeTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(EvolisSdkPrintEncodeTaskService.class);

	static final String IMPRESSION_ET_ENCODAGE_VIA_EVOLIS = "Impression et encodage via imprimante Evolis";

	@Resource
	EvolisSdkPrinterService evolisPrinterService;

	@Resource
	EsupSgcHeartbeatService esupSgcHeartbeatService;

	@Resource
	AppSession appSession;

	@Resource
	private ApplicationContext ctx;

	@Override
	public List<UiStep> getUiStepsList() {
		return EvolisSdkPrintEncodeTask.UI_STEPS_LIST;
	}

	@Override
	protected Task<String> createTask() {
		esupSgcHeartbeatService.setEsupSgcPrinterService(evolisPrinterService);
		esupSgcHeartbeatService.restart();
		return ctx.getBean(EvolisSdkPrintEncodeTask.class, uiSteps, webcamImageView.imageProperty(),  bmpColorImageView, bmpBlackImageView, bmpBackImageView);
	}

	@Override
	protected void cancelled() {
		super.cancelled();
		esupSgcHeartbeatService.cancel();
	}

	@Override
	public AppSession getAppSession() {
		return appSession;
	}

	@Override
	public String getLabel() {
		return IMPRESSION_ET_ENCODAGE_VIA_EVOLIS;
	}

	@Override
	public READY_CONDITION[] readyToRunConditions() {
		return new READY_CONDITION[] {READY_CONDITION.auth, READY_CONDITION.nfc,
				READY_CONDITION.printer, READY_CONDITION.nfc_desfire};
	}


	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView, ImageView bmpBackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView, bmpBackImageView);
		webcamImageView.setVisible(false);
		bmpColorImageView.setVisible(true);
		bmpBlackImageView.setVisible(true);
		bmpBackImageView.setVisible(true);
	}
}
