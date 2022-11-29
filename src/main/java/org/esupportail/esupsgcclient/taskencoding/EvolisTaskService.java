package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisResponse;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class EvolisTaskService extends EsupSgcTaskService<String> {

	private final static Logger log = Logger.getLogger(EvolisTaskService.class);

	RestTemplate restTemplate;

	public EvolisTaskService(TaskParamBean taskParamBean) {
		super(taskParamBean);
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
				updateTitle("En attente...");
				updateProgress(0, 2);
				String qrcode = getQrCode();
				String bmpBlackAsBase64 = EncodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.black);
				String bmpColorAsBase64 = EncodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.color);
				updateProgress(1,10);
				EvolisResponse resp = EvolisPrinterService.insertCardToContactLessStation();
				EncodingService.encode(qrcode);
				updateProgress(1,10);
				updateTitle("Impression ...");
				EvolisPrinterService.printBegin();
				EvolisPrinterService.printSet();
				setUiStepSuccess(UiStep.printer_insert);
				updateProgress(2,10);
				updateTitle("Panneau couleur");
				EvolisPrinterService.printFrontColorBmp(bmpColorAsBase64);
				setUiStepSuccess(UiStep.printer_color);
				updateProgress(3,10);
				updateTitle("Panneau noir");
				EvolisPrinterService.printFrontBlackBmp(bmpBlackAsBase64);
				setUiStepSuccess(UiStep.printer_black);
				updateProgress(4,10);
				updateTitle("Overlay");
				EvolisPrinterService.printFrontVarnish(bmpBlackAsBase64);
				setUiStepSuccess(UiStep.printer_overlay);
				updateProgress(5,10);
				updateTitle("Impression...");
				EvolisPrinterService.print();
				//EvolisPrinterService.printEnd();
				setUiStepSuccess(UiStep.printer_print);
				updateProgress(10,10);
				return null;
			}
		};
		return esupSgcLongPollTask;
	}

	public String getQrCode() {
		while (true) {
			String sgcAuthToken = EsupNfcClientStackPane.sgcAuthToken;
			if (sgcAuthToken != null && !sgcAuthToken.equals("") && !"undefined".equals(sgcAuthToken) && !"null".equals(sgcAuthToken) && EvolisPrinterService.initSocket(false)) {
				try {
					log.debug("Call " + EncodingService.esupSgcUrl + "/wsrest/nfc/qrcode2edit?authToken=" + sgcAuthToken);
					String qrcode = restTemplate.getForObject(EncodingService.esupSgcUrl + "/wsrest/nfc/qrcode2edit?authToken=" + sgcAuthToken, String.class);
					if (qrcode != null) {
						log.debug("qrcode : " + qrcode);
						if("stop".equals(qrcode)) {
							throw new RuntimeException("Un esup-sg-client avec le même utilisateur est déjà démarré ??");
						}
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

	public void setUiStepRunning() {
		for(UiStep step : new UiStep[]{
				UiStep.long_poll,
				UiStep.bmp_black,
				UiStep.bmp_color,
				UiStep.printer_insert,
				UiStep.printer_color,
				UiStep.printer_black,
				UiStep.printer_overlay,
				UiStep.printer_nfc,
				UiStep.csn_read,
				UiStep.encode,
				UiStep.encode_cnous,
				UiStep.send_csv,
				UiStep.printer_eject}) {
			taskParamBean.uiSteps.get(step).setVisible(true);
		}
	}


}
