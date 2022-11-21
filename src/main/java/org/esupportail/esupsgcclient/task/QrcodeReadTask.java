package org.esupportail.esupsgcclient.task;

import java.awt.image.BufferedImage;

import org.esupportail.esupsgcclient.service.webcam.QRCodeReader;
import org.esupportail.esupsgcclient.utils.Utils;

import com.github.sarxos.webcam.WebcamException;

import javafx.embed.swing.SwingFXUtils;
import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.image.Image;

public class QrcodeReadTask extends Task<String> {

	private final ObjectProperty<Image> imageProperty;

	public QrcodeReadTask(final ObjectProperty<Image> imageProperty) {
		this.imageProperty = imageProperty;
	}

	@Override
	protected String call() throws Exception {
		String qrcode = null;
		while (true) {
			if (isCancelled()) break;
			BufferedImage webcamBufferedImage = SwingFXUtils.fromFXImage(imageProperty.get(), null);
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
