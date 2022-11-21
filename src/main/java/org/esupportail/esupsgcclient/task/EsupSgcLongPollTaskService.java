package org.esupportail.esupsgcclient.task;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class EsupSgcLongPollTaskService extends Service<String> {

	private final static Logger log = Logger.getLogger(EsupSgcLongPollTaskService.class);

	RestTemplate restTemplate;

	public EsupSgcLongPollTaskService() {
		super();
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
				while (true) {
					String sgcAuthToken = EsupNfcClientStackPane.sgcAuthToken;
					if (sgcAuthToken != null && !sgcAuthToken.equals("") && !"undefined".equals(sgcAuthToken) && !"null".equals(sgcAuthToken) && EvolisPrinterService.initSocket(false)) {
						try {
							log.debug("Call " + EncodingService.esupSgcUrl + "/wsrest/nfc/qrcode2edit?authToken=" + sgcAuthToken);
							String qrcode = restTemplate.getForObject(EncodingService.esupSgcUrl + "/wsrest/nfc/qrcode2edit?authToken=" + sgcAuthToken, String.class);
							if (qrcode != null) {
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

}
