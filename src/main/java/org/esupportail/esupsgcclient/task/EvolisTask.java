package org.esupportail.esupsgcclient.task;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.ui.MainPane;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class EvolisTask extends Task<Void> {

	private final static Logger log = Logger.getLogger(EvolisTask.class);

	String bmpColorAsBase64;

	String bmpBlackAsBase64;

	public EvolisTask(String bmpColorAsBase64, String bmpBlackAsBase64) {
		this.bmpColorAsBase64 = bmpColorAsBase64;
		this.bmpBlackAsBase64 = bmpBlackAsBase64;
	}

	@Override
	protected Void call() throws Exception {
		try {
			EvolisPrinterService.print(bmpColorAsBase64, bmpBlackAsBase64, "todo");
		} catch(Exception e) {
			throw new RuntimeException("Error printing card : " + e.getMessage(), e);
		}
		return null;
	}

}
