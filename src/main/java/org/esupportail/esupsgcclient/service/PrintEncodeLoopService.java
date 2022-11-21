package org.esupportail.esupsgcclient.service;

import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.task.EsupSgcLongPollTask;
import org.esupportail.esupsgcclient.task.EvolisTask;
import org.springframework.web.client.RestClientException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class PrintEncodeLoopService extends SgcLoopService<String> {

	final static Logger log = Logger.getLogger(PrintEncodeLoopService.class);

	final ImageView bmpColorImageView;

	final ImageView bmpBlackImageView;

	public PrintEncodeLoopService(final ImageView bmpColorImageView, final ImageView bmpBlackImageView) {
		super();
		this.bmpColorImageView = bmpColorImageView;
		this.bmpBlackImageView = bmpBlackImageView;
	}

	@Override
	protected Task<String> createTask() {

		EsupSgcLongPollTask esupSgcLongPollTask = new EsupSgcLongPollTask();
		esupSgcLongPollTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				String qrcode = esupSgcLongPollTask.getValue();
				esupSgcLongPollTask.cancel();
				log.info("Card with qrcode " + qrcode + " should be edited with printer");
				printAndEncode(qrcode);
			}
		});

		return esupSgcLongPollTask;
	}

	private void printAndEncode(String qrcode) {
		String bmpColorAsBase64 = null;
		String bmpBlackAsBase64 = null;
		try {
			bmpColorAsBase64 = EncodingService.getBmpColorAsBase64(qrcode);
			bmpBlackAsBase64 = EncodingService.gatBmpBlackAsBase64(qrcode);
			try {
				byte[] bmpColor = Base64.getDecoder().decode(bmpColorAsBase64.getBytes());
				BufferedImage input_image = ImageIO.read(new ByteArrayInputStream(bmpColor)); //
				ByteArrayOutputStream out = new ByteArrayOutputStream();// read bmp into input_image object
				ImageIO.write(input_image, "PNG", out);
				bmpColorImageView.setImage(new Image(new ByteArrayInputStream( out.toByteArray()), 200, 200, true, true));
				byte[] bmpBlack = Base64.getDecoder().decode(bmpBlackAsBase64.getBytes());
				input_image = ImageIO.read(new ByteArrayInputStream(bmpBlack)); //
				out = new ByteArrayOutputStream();// read bmp into input_image object
				ImageIO.write(input_image, "PNG", out);
				bmpBlackImageView.setImage(new Image(new ByteArrayInputStream( out.toByteArray()), 200, 200, true, true));
			} catch (IOException e) {
				log.warn("Can't display bmp", e);
			}
		} catch(RestClientException ex) {
			throw new RuntimeException("Can't get BMP from esup-sgc for this qrcode " + qrcode, ex);
		}
		EvolisTask evolisTask = new EvolisTask(bmpColorAsBase64, bmpBlackAsBase64);
		evolisTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				log.error("Erreur d'encodage, voir les logs", evolisTask.getException());
				EvolisPrinterService.reject();
				restart();
			}
		});
		evolisTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				encode(qrcode, true);
				//Utils.sleep(3000);
				//EvolisPrinterService.eject();
				//restart();
			}
		});
		Thread evolisThread = new Thread(evolisTask);
		evolisThread.setDaemon(true);
		evolisThread.start();
	}
}
