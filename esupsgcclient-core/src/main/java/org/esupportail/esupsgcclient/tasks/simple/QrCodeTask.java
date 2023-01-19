package org.esupportail.esupsgcclient.tasks.simple;

import com.github.sarxos.webcam.WebcamException;
import javafx.beans.property.ObjectProperty;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.webcam.QRCodeReader;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class QrCodeTask extends EsupSgcTask {

	private final static Logger log = Logger.getLogger(QrCodeTask.class);

	final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{UiStep.qrcode_read, UiStep.encode});

	ObjectProperty<Image> webcamImageProperty;
	EncodingService encodingService;
	QRCodeReader qRCodeReader;

	public QrCodeTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, EncodingService encodingService, QRCodeReader qRCodeReader) {
		super(uiSteps);
		this.webcamImageProperty = webcamImageProperty;
		this.encodingService = encodingService;
		this.qRCodeReader = qRCodeReader;
	}

	@Override
	protected List<UiStep> getUiStepsList() {
		return UI_STEPS_LIST;
	}

	@Override
	protected String call() throws Exception {
		setUiStepRunning();
		setUiStepSuccess(null);
		String qrcode = getQrcode();
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

	public String getQrcode() {
		String qrcode = null;
		while (true) {
			if(isCancelled()) {
				return null;
			}
			BufferedImage webcamBufferedImage = SwingFXUtils.fromFXImage(webcamImageProperty.get(), null);
			qrcode = qRCodeReader.readQrCode(webcamBufferedImage);
			if(webcamBufferedImage != null) {
				if (qrcode != null) {
					break;
				}
			} else {
				throw new WebcamException("no image");
			}
			Utils.sleep(200);
		}
		return qrcode;
	}


}
