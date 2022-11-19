package org.esupportail.esupsgcclient.task;

import javax.smartcardio.CardException;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.domain.NfcResultBean;
import org.esupportail.esupsgcclient.service.pcsc.EncodingException;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.pcsc.PcscUsbService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestTemplate;

import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.scene.control.TextArea;

public class EncodingTask extends Task<String> {

	private final static Logger log = Logger.getLogger(EncodingTask.class);
	private static RestTemplate restTemplate =  new RestTemplate(Utils.clientHttpRequestFactory());
	private String esupNfcTagServerUrl;
	private String numeroId;
	private String cardId;
	private TextArea logTextarea = new TextArea();
	
	public EncodingTask(String esupNfcTagServerUrl, String numeroId, String cardId, TextArea logTextarea) {
		super();
		this.esupNfcTagServerUrl = esupNfcTagServerUrl;
		this.numeroId = numeroId;
		this.cardId = cardId;
		this.logTextarea = logTextarea;
	}

	@Override
    protected String call() throws Exception, EncodingException, PcscException {
		NfcResultBean nfcResultBean;
		log.info("Encoding : Start");
		String result = "";
		while(true){
			log.info("RAPDU : " + result);
			String url = String.format("%s/desfire-ws/?result=%s&numeroId=%s&cardId=%s", esupNfcTagServerUrl, result, numeroId, cardId);
			try {
				nfcResultBean = restTemplate.getForObject(url, NfcResultBean.class);
			} catch(HttpClientErrorException clientEx) {
				throw new EncodingException("Exception during calling esupnfcTagServer", clientEx);
			}
			log.info("SAPDU : "+ nfcResultBean.getFullApdu());
			if(nfcResultBean.getFullApdu()!=null) {
				if(!"END".equals(nfcResultBean.getFullApdu()) ) {
					try {
						result = PcscUsbService.sendAPDU(nfcResultBean.getFullApdu());
				        Platform.runLater(new Runnable() {
				            @Override public void run() {
				            	logTextarea.appendText(".");
				            	logTextarea.positionCaret(logTextarea.getLength());
				            }
				        });
					} catch (CardException e) {
						throw new PcscException("pcsc send apdu error", e);
					}
				} else {
					log.info("Encoding  : OK");
					return nfcResultBean.getFullApdu();
				}
			} else {
				throw new EncodingException("NFC APDU gived by nfctag is null ?!");
			}
		}
    }

}
