package org.esupportail.esupsgcclient.service;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class EsupSgcGetBmpTaskService extends Service<String> {

	final static Logger log = Logger.getLogger(Service.class);

	final String qrcode;

	final EncodingService.BmpType bmpType;

	final ImageView bmpImageView;

	public EsupSgcGetBmpTaskService(String qrcode, EncodingService.BmpType bmpType, ImageView bmpImageView) {
		super();
		this.qrcode = qrcode;
		this.bmpType = bmpType;
		this.bmpImageView = bmpImageView;
	}

	@Override
	protected Task<String> createTask() {

		Task<String> esupSgcGetBmpTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				updateTitle("Récupération de la partie " + (bmpType.equals(EncodingService.BmpType.black) ? "N/B" : "Couleur") + " de la carte");
				String bmpAsBase64 = EncodingService.getBmpAsBase64(qrcode, bmpType);

				// TODO :: do this block outside this thread ?
				byte[] bmp = Base64.getDecoder().decode(bmpAsBase64.getBytes());
				BufferedImage input_image = ImageIO.read(new ByteArrayInputStream(bmp)); //
				ByteArrayOutputStream out = new ByteArrayOutputStream();// read bmp into input_image object
				ImageIO.write(input_image, "PNG", out);
				bmpImageView.setImage(new Image(new ByteArrayInputStream(out.toByteArray()), 200, 200, true, true));
				//

				return bmpAsBase64;
			}
		};
		return esupSgcGetBmpTask;
	}
}
