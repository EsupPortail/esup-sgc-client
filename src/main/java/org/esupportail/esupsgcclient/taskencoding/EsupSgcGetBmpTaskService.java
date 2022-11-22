package org.esupportail.esupsgcclient.taskencoding;

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

public class EsupSgcGetBmpTaskService extends EsupSgcTaskService<String> {

	final static Logger log = Logger.getLogger(Service.class);

	final String qrcode;

	final EncodingService.BmpType bmpType;

	final ImageView bmpColorImageView;

	final ImageView bmpBlackImageView;

	final String bmpColorAsBase64;

	public EsupSgcGetBmpTaskService(ImageView bmpBlackImageView, ImageView bmpColorImageView, String qrcode, EncodingService.BmpType bmpType, String bmpColorAsBase64) {
		super();
		this.bmpColorImageView = bmpColorImageView;
		this.bmpBlackImageView = bmpBlackImageView;
		this.qrcode = qrcode;
		this.bmpType = bmpType;
		this.bmpColorAsBase64 = bmpColorAsBase64;
	}

	@Override
	protected Task<String> createTask() {

		Task<String> esupSgcGetBmpTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				updateProgress(1,2);
				updateTitle("Récupération de la partie " + (bmpType.equals(EncodingService.BmpType.black) ? "N/B" : "Couleur") + " de la carte");
				String bmpAsBase64 = EncodingService.getBmpAsBase64(qrcode, bmpType);

				updateProgress(2,2);
				// TODO :: do this block outside this thread ?
				byte[] bmp = Base64.getDecoder().decode(bmpAsBase64.getBytes());
				BufferedImage input_image = ImageIO.read(new ByteArrayInputStream(bmp)); //
				ByteArrayOutputStream out = new ByteArrayOutputStream();// read bmp into input_image object
				ImageIO.write(input_image, "PNG", out);
				ImageView bmpImageView = bmpType.equals(EncodingService.BmpType.black) ? bmpBlackImageView : bmpColorImageView;
				bmpImageView.setImage(new Image(new ByteArrayInputStream(out.toByteArray()), 200, 200, true, true));
				//
				return bmpAsBase64;
			}
		};
		return esupSgcGetBmpTask;
	}

	@Override
	public EsupSgcTaskService getNext() {
		log.info("getNext " + bmpType);
		if(bmpType.equals(EncodingService.BmpType.color)) {
			String bmpColorAsBase64 = this.getValue();
			return new EsupSgcGetBmpTaskService(bmpBlackImageView, bmpColorImageView, qrcode, EncodingService.BmpType.black, bmpColorAsBase64);
		} else {
			String bmpBlacksBase64 = this.getValue();
			log.info("-> getNext EvolisPrintTaskService : " + bmpColorAsBase64);
			return new EvolisPrintTaskService(qrcode, bmpColorAsBase64, bmpBlacksBase64);
		}
	}
}
