package org.esupportail.esupsgcclient.task;

import javax.smartcardio.CardException;

import javafx.concurrent.Service;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.domain.NfcResultBean;
import org.esupportail.esupsgcclient.service.pcsc.EncodingException;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.pcsc.EsupNgcTagService;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.pcsc.PcscUsbService;

import javafx.application.Platform;
import javafx.concurrent.Task;

public class EncodingTaskService extends Service<String> {
    private final static Logger log = Logger.getLogger(EncodingTaskService.class);
    private String qrcode;

    public EncodingTaskService(String qrcode) {
        super();
        this.qrcode = qrcode;
    }

    @Override
    protected Task<String> createTask() {

        Task<String> encodingTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                updateTitle("Encodage de la carte");
                EncodingService.pcscConnection();
                String csn = EncodingService.readCsn();
                EncodingService.checkBeforeEncoding(qrcode, csn);
                log.info("Encoding : Start");
                String result = "";
                while (true) {
                    log.info("RAPDU : " + result);
                    NfcResultBean nfcResultBean = EsupNgcTagService.getApdu(csn, result);
                    log.info("SAPDU : " + nfcResultBean.getFullApdu());
                    if (nfcResultBean.getFullApdu() != null) {
                        if (!"END".equals(nfcResultBean.getFullApdu())) {
                            try {
                                result = PcscUsbService.sendAPDU(nfcResultBean.getFullApdu());
                                Platform.runLater(new Runnable() {
                                    @Override
                                    public void run() {
                                        // TODO ...
                                        // logTextarea.appendText(".");
                                        // logTextarea.positionCaret(logTextarea.getLength());
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
        };
        return encodingTask;
    }

}
