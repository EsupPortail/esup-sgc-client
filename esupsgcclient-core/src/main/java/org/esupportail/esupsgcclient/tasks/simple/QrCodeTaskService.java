package org.esupportail.esupsgcclient.tasks.simple;

import jakarta.annotation.Resource;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.AppSession.READY_CONDITION;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.webcam.QRCodeReader;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class QrCodeTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(QrCodeTaskService.class);

	static final String ENCODAGE_VIA_SCAN_DE_QR_CODE = "Encodage via scan de QRCode";

	@Resource
	AppSession appSession;

	@Resource
	private ApplicationContext ctx;

	@Override
	public List<UiStep> getUiStepsList() {
		return QrCodeTask.UI_STEPS_LIST;
	}

	@Override
	protected Task<String> createTask() {
		return ctx.getBean(QrCodeTask.class, uiSteps, webcamImageView.imageProperty());
	}

	@Override
	public AppSession getAppSession() {
		return appSession;
	}

	@Override
	public String getLabel() {
		return ENCODAGE_VIA_SCAN_DE_QR_CODE;
	}

	@Override
	public READY_CONDITION[] readyToRunConditions() {
		return new READY_CONDITION[]{READY_CONDITION.auth, READY_CONDITION.nfc, READY_CONDITION.webcam, READY_CONDITION.nfc_desfire};
	}

	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView);
		webcamImageView.setVisible(true);
		bmpColorImageView.setVisible(false);
		bmpBlackImageView.setVisible(false);
	}
}
