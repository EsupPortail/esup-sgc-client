package org.esupportail.esupsgcclient.tasks;

import com.github.sarxos.webcam.WebcamException;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.webcam.QRCodeReader;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class QrCodeTask extends EsupSgcTask {

	private final static Logger log = Logger.getLogger(QrCodeTask.class);
	ObjectProperty<Image> webcamImageProperty;
	EncodingService encodingService;

	public QrCodeTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, EncodingService encodingService) {
		super(uiSteps);
		this.webcamImageProperty = webcamImageProperty;
		this.encodingService = encodingService;
	}

	@Override
	List<UiStep> getUiStepsList() {
		return Arrays.asList(new UiStep[]{
				UiStep.qrcode_read,
				UiStep.encode});
	}

	@Override
	protected String call() throws Exception {
		setUiStepRunning();
		setUiStepSuccess(null);
		String qrcode = getQrcode();
		if(qrcode == null) return null;
		setUiStepRunning();
		setUiStepSuccess(UiStep.qrcode_read);
		try {
			encodingService.encode(qrcode);
			setUiStepSuccess(UiStep.encode);
		} catch (Exception e) {
			setUiStepFailed(UiStep.encode, e);
			updateTitle("Merci de retirer cette carte");
			while (!encodingService.waitForCardAbsent(1000)) {
				// Utils.sleep(1000); -  sleep non nécessaire : EncodingService.waitForCardAbsent l'intègre
			}
			updateTitle("Carte retirée");
		}
		return null;
	}

	public String getQrcode() {
		String qrcode = null;
		while (true) {
			if(isCancelled()) {
				return null;
			}
			BufferedImage webcamBufferedImage = SwingFXUtils.fromFXImage(webcamImageProperty.get(), null);
			qrcode = QRCodeReader.readQrCode(webcamBufferedImage);
			if(webcamBufferedImage != null) {
				if (qrcode != null) {
					break;
				}
			} else {
				throw new WebcamException("no image");
			}
			Utils.sleep(1000);
		}
		return qrcode;
	}


}
