package org.esupportail.esupsgcclient.tasks.simple;

import javax.annotation.Resource;
import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.webcam.QRCodeReader;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class QrCodeTask extends EsupSgcTask {

	private final static Logger log = Logger.getLogger(QrCodeTask.class);

	final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{UiStep.qrcode_read, UiStep.encode});

	@Resource
	EncodingService encodingService;
	@Resource
	QRCodeReader qRCodeReader;

	public QrCodeTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super(uiSteps, webcamImageProperty, bmpColorImageView, bmpBlackImageView);
	}


	@Override
	protected List<UiStep> getUiStepsList() {
		return UI_STEPS_LIST;
	}

	@Override
	protected String call() throws Exception {
		setUiStepRunning();
		setUiStepSuccess(null);
		if(isCancelled()) {
			throw new RuntimeException("Task is cancelled");
		}
		String qrcode = qRCodeReader.getQrcode(this, -1);
		if(qrcode == null) return null;
		long start = System.currentTimeMillis();
		updateTitle4thisTask("qrcode détecté : " + qrcode);
		setUiStepRunning();
		setUiStepSuccess(UiStep.qrcode_read);
		try {
			encodingService.encode(this, qrcode);
			setUiStepSuccess(UiStep.encode);
			String msgTimer = String.format("Carte encodée en %.2f secondes\n", (System.currentTimeMillis()-start)/1000.0);
			updateTitle4thisTask(msgTimer);
		} catch (Exception e) {
			log.debug("Exception on QrCodeTask", e);
			setUiStepFailed(UiStep.encode, e);
			updateTitle4thisTask("PB :" +  e.getMessage());
		} finally {
			updateTitle4thisTask("Merci de retirer cette carte");
			while (!encodingService.waitForCardAbsent(1000)) {
				// Utils.sleep(1000); -  sleep non nécessaire : EncodingService.waitForCardAbsent l'intègre
			}
			updateTitle4thisTask("Carte retirée");
		}
		return null;
	}


}
