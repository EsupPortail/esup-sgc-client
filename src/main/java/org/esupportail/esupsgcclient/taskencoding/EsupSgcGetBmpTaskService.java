package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.ui.UiStep;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;

public class EsupSgcGetBmpTaskService extends EsupSgcTaskService<String> {

	final static Logger log = Logger.getLogger(Service.class);

	public EsupSgcGetBmpTaskService(TaskParamBean taskParamBean) {
		super(taskParamBean);
	}

	UiStep getUiStep() {
		return EncodingService.BmpType.black.equals(taskParamBean.bmpType) ? UiStep.bmp_black : UiStep.bmp_color;
	}

	@Override
	protected Task<String> createTask() {

		Task<String> esupSgcGetBmpTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				updateProgress(1,2);
				updateTitle("Récupération de la partie " + (taskParamBean.bmpType.equals(EncodingService.BmpType.black) ? "N/B" : "Couleur") + " de la carte");
				String bmpAsBase64 = EncodingService.getBmpAsBase64(taskParamBean.qrcode, taskParamBean.bmpType);

				updateProgress(2,2);
				// TODO :: do this block outside this thread ?
				byte[] bmp = Base64.getDecoder().decode(bmpAsBase64.getBytes());
				BufferedImage input_image = ImageIO.read(new ByteArrayInputStream(bmp)); //
				ByteArrayOutputStream out = new ByteArrayOutputStream();// read bmp into input_image object
				ImageIO.write(input_image, "PNG", out);
				ImageView bmpImageView = taskParamBean.bmpType.equals(EncodingService.BmpType.black) ? taskParamBean.bmpBlackImageView : taskParamBean.bmpColorImageView;
				bmpImageView.setImage(new Image(new ByteArrayInputStream(out.toByteArray()), 200, 200, true, true));
				//
				return bmpAsBase64;
			}
		};
		return esupSgcGetBmpTask;
	}

	@Override
	public EsupSgcTaskService getNextWhenSuccess() {
		log.info("getNext " + taskParamBean.bmpType);
		if(taskParamBean.bmpType.equals(EncodingService.BmpType.black)) {
			String bmpColorAsBase64 = this.getValue();
			return new EsupSgcGetBmpTaskService(new TaskParamBean(taskParamBean.uiSteps, taskParamBean.rootType, taskParamBean.qrcode, taskParamBean.webcamImageProperty, taskParamBean.csn,
					EncodingService.BmpType.color, taskParamBean.bmpColorImageView, taskParamBean.bmpBlackImageView,
					bmpColorAsBase64, taskParamBean.bmpBlackAsBase64,
					taskParamBean.eject4success, taskParamBean.fromPrinter));
		} else {
			String bmpBlackAsBase64 = this.getValue();
			return new EvolisPrintTaskService(new TaskParamBean(taskParamBean.uiSteps, taskParamBean.rootType,taskParamBean.qrcode, taskParamBean.webcamImageProperty, taskParamBean.csn,
					taskParamBean.bmpType, taskParamBean.bmpColorImageView, taskParamBean.bmpBlackImageView,
					taskParamBean.bmpColorAsBase64, bmpBlackAsBase64,
					taskParamBean.eject4success, taskParamBean.fromPrinter));
		}
	}
}
