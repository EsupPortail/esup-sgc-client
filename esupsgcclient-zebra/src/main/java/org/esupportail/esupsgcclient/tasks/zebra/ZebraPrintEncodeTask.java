package org.esupportail.esupsgcclient.tasks.zebra;

import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
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

    private final static Logger log = Logger.getLogger(ZebraPrintEncodeTask.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.long_poll,
            UiStep.bmp_black,
            UiStep.bmp_color,
            UiStep.printer_print,
            UiStep.printer_nfc,
            UiStep.encode});

    ImageView bmpColorImageView;

    ImageView bmpBlackImageView;

    @Resource
    EsupSgcRestClientService esupSgcRestClientService;
    @Resource
    ZebraPrinterService zebraPrinterService;
    @Resource
    EncodingService encodingService;
    public ZebraPrintEncodeTask(Map<UiStep, TextFlow> uiSteps, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
        super(uiSteps);
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
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
            String bmpOverlayAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.overlay);
            zebraPrinterService.print(bmpBlackAsBase64, bmpColorAsBase64, bmpOverlayAsBase64);
            setUiStepSuccess(UiStep.printer_print);
            zebraPrinterService.launchEncoding();
            setUiStepSuccess(UiStep.printer_nfc);
            encodingService.encode(this);
            setUiStepSuccess(UiStep.encode);
            zebraPrinterService.eject();
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            zebraPrinterService.cancelJobs();
            //zebraPrinterService.reset();
            updateTitle("Carte rejetée");
            throw new RuntimeException("Exception on  ZebraReadNfcTask : " + e.getMessage(), e);
        } finally {
            zebraPrinterService.cancelJob();
        }
        updateTitle4thisTask("ZebraReadNfcTask OK");
		return null;
	}

}
