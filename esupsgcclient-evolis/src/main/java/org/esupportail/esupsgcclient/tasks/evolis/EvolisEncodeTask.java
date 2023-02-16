package org.esupportail.esupsgcclient.tasks.evolis;

import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcRestClientService;
import org.esupportail.esupsgcclient.service.webcam.QRCodeReader;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class EvolisEncodeTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(EvolisEncodeTask.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.printer_nfc,
            UiStep.printer_print,
            UiStep.qrcode_read,
            UiStep.encode
    });

    @Resource
    EncodingService encodingService;

    @Resource
    EvolisPrinterService evolisPrinterService;

    @Resource
    QRCodeReader qRCodeReader;

    public EvolisEncodeTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
        super(uiSteps, webcamImageProperty, bmpColorImageView, bmpBlackImageView);
    }


    @Override
    protected List<UiStep> getUiStepsList() {
        return UI_STEPS_LIST;
    }

    @Override
    protected String call() throws Exception {
        try {
            setUiStepRunning();
            setUiStepSuccess(null);
            long start = System.currentTimeMillis();
            evolisPrinterService.insertCardToContactLessStation(this);
            String evolisPrinterStatus = evolisPrinterService.getPrinterStatus().getResult();
            while(!evolisPrinterStatus.contains("ENCODING_RUNNING")) {
                updateTitle(String.format("en attente d'une carte ...", evolisPrinterStatus));
                Utils.sleep(500);
                evolisPrinterStatus = evolisPrinterService.getPrinterStatus().getResult();
            }
            setUiStepSuccess(UiStep.printer_nfc);
            encodingService.pcscConnection(this);
            encodingService.waitForCardPresent(5000);
            String csn = encodingService.readCsn();
            updateTitle4thisTask(csn);
            setUiStepSuccess(UiStep.printer_nfc);
            String qrcode = qRCodeReader.getQrcode(this, 50);
            if(qrcode == null) {
                updateTitle4thisTask("qrcode non détecté");
                evolisPrinterService.reject();
            } else {
                updateTitle4thisTask("qrcode détecté : " + qrcode);
                setUiStepRunning();
                setUiStepSuccess(UiStep.qrcode_read);
                encodingService.encode(this, qrcode);
                setUiStepSuccess(UiStep.encode);
                evolisPrinterService.eject();
            }
            String msgTimer = String.format("Carte encodée en %.2f secondes\n", (System.currentTimeMillis() - start) / 1000.0);
            updateTitle(msgTimer);
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            evolisPrinterService.reject();
        } 
		return null;
	}

}
