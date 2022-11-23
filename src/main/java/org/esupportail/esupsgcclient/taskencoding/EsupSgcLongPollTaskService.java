package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Task;
import javafx.event.EventDispatchChain;
import javafx.scene.image.ImageView;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class EsupSgcLongPollTaskService extends EsupSgcTaskService<String> {

	private final static Logger log = Logger.getLogger(EsupSgcLongPollTaskService.class);

	static long lastRunTime = 100000;

	RestTemplate restTemplate;

	ImageView bmpBlackImageView;
	ImageView bmpColorImageView;

	public EsupSgcLongPollTaskService(ImageView bmpBlackImageView, ImageView bmpColorImageView) {
		super();
		this.bmpColorImageView = bmpColorImageView;
		this.bmpBlackImageView = bmpBlackImageView;
		HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		httpRequestFactory.setConnectionRequestTimeout(300000);
		httpRequestFactory.setConnectTimeout(300000);
		httpRequestFactory.setReadTimeout(300000);
		restTemplate = new RestTemplate(httpRequestFactory);
	}

	@Override
	protected Task<String> createTask() {
		Task<String> esupSgcLongPollTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				bmpBlackImageView.setImage(null);
				bmpColorImageView.setImage(null);
				updateTitle("En attente...");
				updateProgress(0, 2);
				while (true) {
					String sgcAuthToken = EsupNfcClientStackPane.sgcAuthToken;
					if (sgcAuthToken != null && !sgcAuthToken.equals("") && !"undefined".equals(sgcAuthToken) && !"null".equals(sgcAuthToken) && EvolisPrinterService.initSocket(false)) {
						try {
							log.debug("Call " + EncodingService.esupSgcUrl + "/wsrest/nfc/qrcode2edit?authToken=" + sgcAuthToken);
							String qrcode = restTemplate.getForObject(EncodingService.esupSgcUrl + "/wsrest/nfc/qrcode2edit?authToken=" + sgcAuthToken, String.class);
							if (qrcode != null) {
								updateProgress(2, 2);
								return qrcode;
							}
						} catch (ResourceAccessException e) {
							log.debug("timeout ... we recall esup-sgc in 2 sec");
							Utils.sleep(2000);
						}
					} else {
						Utils.sleep(1000);
					}
				}
			}
		};
		return esupSgcLongPollTask;
	}

	@Override
	public EsupSgcTaskService getNext() {
		String qrcode = this.getValue();
		return new EsupSgcGetBmpTaskService(bmpBlackImageView, bmpColorImageView, qrcode, EncodingService.BmpType.color,null);
	}

}
