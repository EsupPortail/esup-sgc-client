package org.esupportail.esupsgcclient.tasks;

import com.github.sarxos.webcam.WebcamException;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.webcam.QRCodeReader;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.awt.image.BufferedImage;
import java.util.Map;

@Component
public class QrCodeTaskService extends EsupSgcTaskService<String> {

	private final static Logger log = Logger.getLogger(QrCodeTaskService.class);

	ObjectProperty<Image> webcamImageProperty;

	@Resource
	EncodingService encodingService;

	public void init(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty) {
		super.init(uiSteps);
		this.webcamImageProperty = webcamImageProperty;
	}

	@Override
	protected Task<String> createTask() {
		Task<String> qrcodeEncodeTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				updateTitle("En attente...");
				updateProgress(0, 2);
				String qrcode = getQrcode();
				try {
					encodingService.encode(qrcode);
				} catch (Exception e) {
					updateTitle("Exception : " + e.getMessage());
					updateTitle("Merci de retirer cette carte");
					while (!encodingService.waitForCardAbsent(1000)) {
						// Utils.sleep(1000); -  sleep non nécessaire : EncodingService.waitForCardAbsent l'intègre
					}
					updateTitle("Carte retirée");
				}
				return null;
			}
		};
		return qrcodeEncodeTask;
	}

	public String getQrcode() {
		String qrcode = null;
		while (true) {
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

	public void setUiStepSuccess() {
		for(UiStep step : new UiStep[]{
				UiStep.csn_read,
				UiStep.qrcode_read,
				UiStep.csn_read,
				UiStep.sgc_select,
				UiStep.encode,
				UiStep.encode_cnous,
				UiStep.send_csv}) {
			uiSteps.get(step).setVisible(true);
		}
	}

}
