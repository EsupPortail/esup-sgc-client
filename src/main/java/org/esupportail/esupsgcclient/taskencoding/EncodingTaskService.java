package org.esupportail.esupsgcclient.taskencoding;

import javax.smartcardio.CardException;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.NfcResultBean;
import org.esupportail.esupsgcclient.service.pcsc.EncodingException;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.pcsc.EsupNgcTagService;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.pcsc.PcscUsbService;

import javafx.concurrent.Task;
import org.esupportail.esupsgcclient.utils.Utils;

public class EncodingTaskService extends EsupSgcTaskService<String> {

    private final static Logger log = Logger.getLogger(EncodingTaskService.class);

    static long lastRunTime = 5000;

    public EncodingTaskService(TaskParamBean taskParamBean) {
        super(taskParamBean);
        assert taskParamBean.qrcode != null;
        assert taskParamBean.fromPrinter != null;
    }

    @Override
    protected Task<String> createTask() {
        Task<String> encodingTask = new Task<String>() {
            @Override
            protected String call() throws Exception {
                long start = System.currentTimeMillis();
                long t;
                updateProgress(1, 10);
                updateTitle("Connexion au terminal NFC disposant d'une carte");
                while(!EncodingService.pcscConnection()) {
                    Utils.sleep(1000);
                }
                updateTitle("Encodage de la carte");
                String csn = EncodingService.readCsn();
                EncodingService.checkBeforeEncoding(taskParamBean.qrcode, csn);
                log.info("Encoding : Start");
                String result = "";
                while (true) {
                    t = System.currentTimeMillis() - start;
                    updateProgress(t, Math.max(lastRunTime-1000, t));
                    log.info("RAPDU : " + result);
                    NfcResultBean nfcResultBean = EsupNgcTagService.getApdu(csn, result);
                    log.info("SAPDU : " + nfcResultBean.getFullApdu());
                    if (nfcResultBean.getFullApdu() != null) {
                        if (!"END".equals(nfcResultBean.getFullApdu())) {
                            try {
                                result = PcscUsbService.sendAPDU(nfcResultBean.getFullApdu());
                                //updateProgress((long)this.getProgress()+1, (long)this.getTotalWork()+1);
                            } catch (CardException e) {
                                throw new PcscException("pcsc send apdu error", e);
                            }
                        } else {
                            log.info("Encoding  : OK");
                            lastRunTime = t;
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

    @Override
    public EsupSgcTaskService getNextWhenSuccess() {
        if(taskParamBean.fromPrinter) {
            return new EvolisEjectTaskService(taskParamBean);
        } else {
            return new WaitRemoveCardTaskService(taskParamBean);
        }
    }

}
