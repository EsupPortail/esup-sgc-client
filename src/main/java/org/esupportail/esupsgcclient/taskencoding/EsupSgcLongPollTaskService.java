package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

public class EsupSgcLongPollTaskService extends EsupSgcTaskService<String> {

	private final static Logger log = Logger.getLogger(EsupSgcLongPollTaskService.class);

	static long lastRunTime = 100000;

	RestTemplate restTemplate;

	public EsupSgcLongPollTaskService(TaskParamBean taskParamBean) {
		super(taskParamBean);
		HttpComponentsClientHttpRequestFactory httpRequestFactory = new HttpComponentsClientHttpRequestFactory();
		httpRequestFactory.setConnectionRequestTimeout(300000);
		httpRequestFactory.setConnectTimeout(300000);
		httpRequestFactory.setReadTimeout(300000);
		restTemplate = new RestTemplate(httpRequestFactory);
	}

	public boolean isRoot() {
		return true;
	}

	public UiStep getUiStep() {
		return UiStep.long_poll;
	}

	@Override
	protected Task<String> createTask() {
		Task<String> esupSgcLongPollTask = new Task<String>() {
			@Override
			protected String call() throws Exception {
				taskParamBean.bmpBlackImageView.setImage(null);
				taskParamBean.bmpColorImageView.setImage(null);
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
								log.debug("qrcode : " + qrcode);
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

	public void setUiStepSuccess() {
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
		super.setUiStepSuccess();
	}

	@Override
	public EsupSgcTaskService getNextWhenSuccess() {
		String qrcode = this.getValue();
		return new EsupSgcGetBmpTaskService(new TaskParamBean(taskParamBean.uiSteps, taskParamBean.rootType, qrcode, taskParamBean.webcamImageProperty, taskParamBean.csn,
				taskParamBean.bmpType, taskParamBean.bmpColorImageView, taskParamBean.bmpBlackImageView,
				taskParamBean.bmpColorAsBase64, taskParamBean.bmpBlackAsBase64,
				taskParamBean.eject4success, taskParamBean.fromPrinter));
	}

}
