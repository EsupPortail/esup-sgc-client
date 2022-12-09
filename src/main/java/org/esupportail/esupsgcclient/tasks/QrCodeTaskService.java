package org.esupportail.esupsgcclient.tasks;

import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.webcam.QRCodeReader;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class QrCodeTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(QrCodeTaskService.class);

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
	public boolean isReadyToRun() {
		return appSession.isAuthReady() && appSession.isNfcReady() && appSession.isWebcamReady() && "DESFIRE".equals(appSession.getAuthType());
	}

	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView);
		webcamImageView.setVisible(true);
		bmpColorImageView.setVisible(false);
		bmpBlackImageView.setVisible(false);
	}
}
