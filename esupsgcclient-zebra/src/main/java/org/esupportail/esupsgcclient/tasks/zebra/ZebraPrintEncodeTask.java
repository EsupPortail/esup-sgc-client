package org.esupportail.esupsgcclient.tasks.zebra;

import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.zebra.ZebraPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcRestClientService;
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
public class ZebraPrintEncodeTask extends EsupSgcTask {

    private final static Logger log = LoggerFactory.getLogger(ZebraPrintEncodeTask.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.long_poll,
            UiStep.bmp_black,
            UiStep.bmp_color,
            UiStep.bmp_back,
            UiStep.printer_print,
            UiStep.printer_nfc,
            UiStep.encode});

    @Resource
    EsupSgcRestClientService esupSgcRestClientService;
    @Resource
    ZebraPrinterService zebraPrinterService;
    @Resource
    EncodingService encodingService;

    public ZebraPrintEncodeTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, ImageView bmpColorImageView, ImageView bmpBlackImageView, ImageView bmpBackImageView) {
        super(uiSteps, webcamImageProperty, bmpColorImageView, bmpBlackImageView, bmpBackImageView);
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
            String bmpBackAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.back);
            if(StringUtils.isNotEmpty(bmpBackAsBase64)) {
                updateBmpUi(bmpBackAsBase64, bmpBackImageView);
                setUiStepSuccess(UiStep.bmp_back);
            }
            String bmpOverlayAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.overlay);
            zebraPrinterService.print(bmpBlackAsBase64, bmpColorAsBase64, bmpBackAsBase64, bmpOverlayAsBase64);
            setUiStepSuccess(UiStep.printer_print);
            zebraPrinterService.launchEncoding();
            setUiStepSuccess(UiStep.printer_nfc);
            encodingService.encode(this, qrcode);
            setUiStepSuccess(UiStep.encode);
            zebraPrinterService.eject();
            String msgTimer = String.format("Carte éditée en %.2f secondes\n", (System.currentTimeMillis()-start)/1000.0);
            updateTitle(msgTimer);
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            zebraPrinterService.cancelJobs();
            updateTitle("Carte rejetée");
            throw new RuntimeException("Exception on ZebraPrintEncodeTask : " + e.getMessage(), e);
        } finally {
            zebraPrinterService.cancelJob();
            resetBmpUi();
        }
		return null;
	}

}
