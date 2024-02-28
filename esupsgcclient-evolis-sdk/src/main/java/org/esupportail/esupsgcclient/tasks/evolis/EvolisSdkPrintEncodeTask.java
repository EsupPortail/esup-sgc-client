package org.esupportail.esupsgcclient.tasks.evolis;

import javax.annotation.Resource;

import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisSdkPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcRestClientService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class EvolisSdkPrintEncodeTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(EvolisSdkPrintEncodeTask.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.long_poll,
            UiStep.bmp_black,
            UiStep.bmp_color,
            UiStep.printer_color,
            UiStep.printer_black,
            UiStep.printer_print,
            UiStep.printer_nfc,
            UiStep.encode
    });

    @Resource
    EsupSgcRestClientService esupSgcRestClientService;

    @Resource
    EncodingService encodingService;

    @Resource
    EvolisSdkPrinterService evolisPrinterService;

    public EvolisSdkPrintEncodeTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
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
            if(isCancelled()) {
                throw new RuntimeException("Task is cancelled");
            }
            log.debug("try to get qrcode ...");
            String qrcode = esupSgcRestClientService.getQrCode(this, null);
            long start = System.currentTimeMillis();
            setUiStepSuccess(UiStep.long_poll);
            String bmpBlackAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.black);
            updateBmpUi(bmpBlackAsBase64, bmpBlackImageView);
            setUiStepSuccess(UiStep.bmp_black);
            String bmpColorAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.color);
            updateBmpUi(bmpColorAsBase64, bmpColorImageView);
            setUiStepSuccess(UiStep.bmp_color);
            evolisPrinterService.releaseIfNeeded();
            String evolisPrinterStatus = evolisPrinterService.getPrinterStatus();
            while(!evolisPrinterStatus.contains("PRINTER_READY")) {
                updateTitle(String.format("Status de l'imprimante evolis non prête (%s) - en attente ...", evolisPrinterStatus));
                Utils.sleep(5000);
                evolisPrinterStatus = evolisPrinterService.getPrinterStatus();
            }
            evolisPrinterService.startSequence();
            if(!evolisPrinterService.printFrontColorBmp(bmpColorAsBase64)) {
                throw new RuntimeException("Impossible d'imprimer le bmp couleur");
            }
            setUiStepSuccess(UiStep.printer_color);
            if(!evolisPrinterService.printFrontBlackBmp(bmpBlackAsBase64)) {
                throw new RuntimeException("Impossible d'imprimer le bmp noir");
            }
            setUiStepSuccess(UiStep.printer_black);
            evolisPrinterService.print();
            setUiStepSuccess(UiStep.printer_print);
            while(!evolisPrinterService.insertCardToContactLessStation()) {
                updateTitle("Impossible d'insérer la carte dans la station NFC - en attente ...");
                Utils.sleep(500);
            }
            evolisPrinterStatus = evolisPrinterService.getPrinterStatus();
            while(!evolisPrinterStatus.contains("ENCODING_RUNNING")) {
                updateTitle(String.format("en attente d'une carte ...", evolisPrinterStatus));
                Utils.sleep(500);
                evolisPrinterStatus = evolisPrinterService.getPrinterStatus();
            }
            setUiStepSuccess(UiStep.printer_nfc);
            encodingService.pcscConnection(this);
            encodingService.waitForCardPresent(5000);
            String csn = encodingService.readCsn();
            updateTitle4thisTask(csn);
            encodingService.encode(this, qrcode);
            setUiStepSuccess(UiStep.encode);
            evolisPrinterService.eject();
            String msgTimer = String.format("Carte éditée en %.2f secondes\n", (System.currentTimeMillis()-start)/1000.0);
            updateTitle(msgTimer);
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            throw new RuntimeException("Exception on  EvolisTask : " + e.getMessage(), e);
        } finally {
            resetBmpUi();
        }
		return null;
	}

}
