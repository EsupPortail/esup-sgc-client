package org.esupportail.esupsgcclient.tasks.zebra;

import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.AppSession.READY_CONDITION;
import org.esupportail.esupsgcclient.service.printer.zebra.ZebraPrinterService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class ZebraEncodeTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(ZebraEncodeTaskService.class);

	static final String ROBOT_ENCODAGE_VIA_ZEBRA = "Robot encodage via Webcam et Imprimante Zebra";

	@Resource
	private ApplicationContext ctx;

	@Resource
	AppSession appSession;

	@Override
	public List<UiStep> getUiStepsList() {
		return ZebraEncodeTask.UI_STEPS_LIST;
	}

	@Override
	protected Task<String> createTask() {
		return ctx.getBean(ZebraEncodeTask.class, uiSteps, webcamImageView.imageProperty(),  bmpColorImageView, bmpBlackImageView);
	}

	@Override
	public AppSession getAppSession() {
		return appSession;
	}

	@Override
	public String getLabel() {
		return ROBOT_ENCODAGE_VIA_ZEBRA;
	}

	@Override
	public READY_CONDITION[] readyToRunConditions() {
		return new  READY_CONDITION[] { READY_CONDITION.auth, READY_CONDITION.nfc, READY_CONDITION.webcam, READY_CONDITION.printer, READY_CONDITION.nfc_desfire};
	}

	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView, ImageView bmpBackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView, bmpBackImageView);
		webcamImageView.setVisible(true);
		bmpColorImageView.setVisible(false);
		bmpBlackImageView.setVisible(false);
		bmpBackImageView.setVisible(false);
	}
}
