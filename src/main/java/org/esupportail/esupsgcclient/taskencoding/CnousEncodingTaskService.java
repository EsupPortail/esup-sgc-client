package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.utils.Utils;

public class CnousEncodingTaskService extends Service<String> {

    private final static Logger log = Logger.getLogger(QrCodeTaskService.class);

    final String csn;

    public CnousEncodingTaskService(String csn) {
        this.csn = csn;
    }

    @Override
    protected Task<String> createTask() {

        Task<String> cnousEncodingTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                if (EncodingService.isCnousOK() && EncodingService.isEncodeCnous()) {
                    log.info("Cnous Encoding :  Start");
                    EncodingService.delCnousCsv();
                    if (EncodingService.cnousEncoding(csn)) {
                        log.info("cnous encoding : OK");
                        if (EncodingService.sendCnousCsv(csn)) {
                            log.info("cnous csv send : OK");
                            Utils.playSound("success.wav");
                        } else {
                            log.warn("Cnous csv send :  Failed");
                        }
                    } else {
                        log.warn("cnous csv send : Failed for csn " + csn, null);
                    }
                } else {
                    log.info("Cnous Encoding :  Skipped");
                    log.info("Encodage terminé");
                    Utils.playSound("success.wav");
                }
                return csn;
            }
        };
        return cnousEncodingTask;
    }

}
