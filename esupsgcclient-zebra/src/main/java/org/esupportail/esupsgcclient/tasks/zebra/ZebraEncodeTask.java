package org.esupportail.esupsgcclient.tasks.zebra;

import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.zebra.ZebraPrinterService;
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
public class ZebraEncodeTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(ZebraEncodeTask.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.printer_nfc,
            UiStep.qrcode_read,
            UiStep.encode});

    @Resource
    ZebraPrinterService zebraPrinterService;
    @Resource
    EncodingService encodingService;

    @Resource
    QRCodeReader qRCodeReader;

    public ZebraEncodeTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
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
            long start = System.currentTimeMillis();
            zebraPrinterService.launchEncoding();
           String zebraStatus = zebraPrinterService.getStatus();
            updateTitle4thisTask(zebraStatus);
            if(zebraStatus.contains("Out of cards")) {
                throw new RuntimeException("Out of cards");
            }
            setUiStepSuccess(UiStep.printer_nfc);
            String qrcode = qRCodeReader.getQrcode(this, 50);
            if(qrcode == null) {
                updateTitle4thisTask("qrcode non détecté");
                zebraPrinterService.reject();
            } else {
                updateTitle4thisTask("qrcode détecté : " + qrcode);
                setUiStepRunning();
                setUiStepSuccess(UiStep.qrcode_read);
                encodingService.encode(this, qrcode);
                setUiStepSuccess(UiStep.encode);
                zebraPrinterService.eject();
                String msgTimer = String.format("Carte encodée en %.2f secondes\n", (System.currentTimeMillis() - start) / 1000.0);
                updateTitle(msgTimer);
            }
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            zebraPrinterService.reject();
            throw new RuntimeException("Exception on ZebraEncodeTask : " + e.getMessage(), e);
        } finally {
            zebraPrinterService.cancelJob();
            updateTitle("Carte rejetée");
            resetBmpUi();
        }
		return null;
	}

}
