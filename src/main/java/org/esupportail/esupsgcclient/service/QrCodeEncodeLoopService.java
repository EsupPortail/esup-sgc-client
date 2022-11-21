package org.esupportail.esupsgcclient.service;

import javafx.application.Platform;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import javafx.scene.image.Image;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.EncodingException;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.task.EncodingTask;
import org.esupportail.esupsgcclient.task.EsupSgcLongPollTask;
import org.esupportail.esupsgcclient.task.EvolisTask;
import org.esupportail.esupsgcclient.task.QrcodeReadTask;
import org.esupportail.esupsgcclient.task.WaitRemoveCardTask;
import org.esupportail.esupsgcclient.ui.MainController;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.web.client.RestClientException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class QrCodeEncodeLoopService extends SgcLoopService<String> {

	private final static Logger log = Logger.getLogger(QrCodeEncodeLoopService.class);

	final ObjectProperty<Image> imageProperty;

	public QrCodeEncodeLoopService(final ObjectProperty<Image> imageProperty) {
		super();
		this.imageProperty = imageProperty;
	}

	@Override
	protected Task<String> createTask() {

		QrcodeReadTask qrcodeReadTask = new QrcodeReadTask(imageProperty);
		qrcodeReadTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				String qrcode = qrcodeReadTask.getValue();
				qrcodeReadTask.cancel();
				if(qrcode!=null) {
					encode(qrcode, false);
				}
			}
		});

		return qrcodeReadTask;
	}

}
