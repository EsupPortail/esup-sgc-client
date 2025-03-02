package org.esupportail.esupsgcclient.tasks.zebra;

import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.AppSession.READY_CONDITION;
import org.esupportail.esupsgcclient.service.printer.zebra.ZebraPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcHeartbeatService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class ZebraPrintEncodeTaskService extends EsupSgcTaskService {

	private final static Logger log = LoggerFactory.getLogger(ZebraPrintEncodeTaskService.class);

	static final String IMPRESSION_ET_ENCODAGE__VIA_ZEBRA = "Impression et encodage via Imprimante Zebra";

	@Resource
	ZebraPrinterService zebraPrinterService;

	@Resource
	EsupSgcHeartbeatService esupSgcHeartbeatService;

	@Resource
	private ApplicationContext ctx;

	@Resource
	AppSession appSession;

	@Override
	public List<UiStep> getUiStepsList() {
		return ZebraPrintEncodeTask.UI_STEPS_LIST;
	}

	@Override
	protected Task<String> createTask() {
		esupSgcHeartbeatService.setEsupSgcPrinterService(zebraPrinterService);
		esupSgcHeartbeatService.restart();
		return ctx.getBean(ZebraPrintEncodeTask.class, uiSteps, webcamImageView.imageProperty(),  bmpColorImageView, bmpBlackImageView, bmpBackImageView);
	}

	@Override
	public AppSession getAppSession() {
		return appSession;
	}

	@Override
	public String getLabel() {
		return IMPRESSION_ET_ENCODAGE__VIA_ZEBRA;
	}

	@Override
	public READY_CONDITION[] readyToRunConditions() {
		return new  READY_CONDITION[] { READY_CONDITION.auth, READY_CONDITION.nfc, READY_CONDITION.printer};
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
