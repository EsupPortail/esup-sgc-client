package org.esupportail.esupsgcclient.taskencoding;

import com.github.sarxos.webcam.WebcamException;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.webcam.QRCodeReader;
import org.esupportail.esupsgcclient.utils.Utils;

import java.awt.image.BufferedImage;

public class QrCodeTaskService extends EsupSgcTaskService<String> {

	private final static Logger log = Logger.getLogger(QrCodeTaskService.class);

	final ObjectProperty<Image> imageProperty;

	public QrCodeTaskService(final ObjectProperty<Image> imageProperty) {
		super();
		this.imageProperty = imageProperty;
	}

	@Override
	protected Task<String> createTask() {
		Task<String> qrcodeEncodeTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				updateProgress(1, 2);
				String qrcode = null;
				while (true) {
					updateTitle("En attente...");
					if (isCancelled()) break;
					BufferedImage webcamBufferedImage = SwingFXUtils.fromFXImage(imageProperty.get(), null);
					qrcode = QRCodeReader.readQrCode(webcamBufferedImage);
					if(webcamBufferedImage != null) {
						if (qrcode != null) {
							updateProgress(2, 2);
							break;
						}
					} else {
						throw new WebcamException("no image");
					}
					Utils.sleep(1000);
				}
				return qrcode;
			}
		};
		return qrcodeEncodeTask;
	}

	@Override
	public EsupSgcTaskService getNext() {
		String qrcode = this.getValue();
		return new EncodingTaskService(qrcode, false);
	}

}
