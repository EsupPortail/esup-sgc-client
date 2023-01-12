package org.esupportail.esupsgcclient.tasks.simple;

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
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Map;

@Component
public class QrCodeTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(QrCodeTaskService.class);

	static final String ENCODAGE_VIA_SCAN_DE_QR_CODE = "Encodage via scan de QRCode";

	@Resource
	EncodingService encodingService;

	@Resource
	QRCodeReader qRCodeReader;

	@Resource
	AppSession appSession;

	@Override
	protected Task<String> createTask() {
		return new QrCodeTask(uiSteps, webcamImageView.imageProperty(), encodingService, qRCodeReader);
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
