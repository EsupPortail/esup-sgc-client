package org.esupportail.esupsgcclient.tasks;

import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcRestClientService;
import org.esupportail.esupsgcclient.ui.UiStep;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class EvolisEncodePrintTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(EvolisEncodePrintTaskService.class);

    ImageView bmpColorImageView;

    ImageView bmpBlackImageView;
    EsupSgcRestClientService esupSgcRestClientService;
    EvolisPrinterService evolisPrinterService;
    EncodingService encodingService;
    public EvolisEncodePrintTask(Map<UiStep, TextFlow> uiSteps, ImageView bmpColorImageView, ImageView bmpBlackImageView,
                                 EsupSgcRestClientService esupSgcRestClientService, EvolisPrinterService evolisPrinterService, EncodingService encodingService) {
        super(uiSteps);
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
        this.esupSgcRestClientService = esupSgcRestClientService;
        this.evolisPrinterService = evolisPrinterService;
        this.encodingService = encodingService;
    }

    @Override
    List<UiStep> getUiStepsList() {
        return Arrays.asList(new UiStep[]{
                UiStep.long_poll,
                UiStep.bmp_black,
                UiStep.bmp_color,
                UiStep.printer_nfc,
                UiStep.encode,
                UiStep.printer_color,
                UiStep.printer_black,
                UiStep.printer_overlay,
                UiStep.printer_print,
                UiStep.sgc_ok});
    }

    @Override
    protected String call() throws Exception {
        try {
            setUiStepRunning();
            setUiStepSuccess(null);
            log.debug("reject and printEnd if needed - usually not necessary");
            evolisPrinterService.reject();
            evolisPrinterService.try2printEnd();
            log.debug("try to get qrcode ...");
            String qrcode = esupSgcRestClientService.getQrCode(this);
            setUiStepRunning();
            setUiStepSuccess(UiStep.long_poll);
            String bmpBlackAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.black);
            updateBmpUi(bmpBlackAsBase64, bmpBlackImageView);
            setUiStepSuccess(UiStep.bmp_black);
            String bmpColorAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.color);
            updateBmpUi(bmpColorAsBase64, bmpColorImageView);
            setUiStepSuccess(UiStep.bmp_color);
            String bmpOverlayAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.overlay);
            evolisPrinterService.insertCardToContactLessStation(this);
            setUiStepSuccess(UiStep.printer_nfc);
            encodingService.pcscConnection(this);
            encodingService.waitForCardPresent(5000);
            String csn = encodingService.readCsn();
            encodingService.encode(this, qrcode);
            setUiStepSuccess(UiStep.encode);
            evolisPrinterService.printBegin();
            evolisPrinterService.printSet();
            evolisPrinterService.printFrontColorBmp(bmpColorAsBase64);
            setUiStepSuccess(UiStep.printer_color);
            evolisPrinterService.printFrontBlackBmp(bmpBlackAsBase64);
            setUiStepSuccess(UiStep.printer_black);
            evolisPrinterService.printFrontVarnish(bmpOverlayAsBase64);
            setUiStepSuccess(UiStep.printer_overlay);
            evolisPrinterService.print();
            setUiStepSuccess(UiStep.printer_print);
            esupSgcRestClientService.setCardEncodedPrinted(csn, qrcode);
            setUiStepSuccess(UiStep.sgc_ok);
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            throw new RuntimeException("Exception on  EvolisTask : " + e.getMessage(), e);
        } finally {
            try {
                evolisPrinterService.printEnd();
                resetBmpUi();
            } catch (Exception e) {
                log.info("Can't reset printing session after exception", e);
            }
        }
		return null;
	}

    private void resetBmpUi() {
        bmpColorImageView.setImage(null);
        bmpBlackImageView.setImage(null);
    }

    private void updateBmpUi(String bmpAsBase64, ImageView bmpImageView) {
        try {
            byte[] bmp = Base64.getDecoder().decode(bmpAsBase64.getBytes());
            BufferedImage input_image = ImageIO.read(new ByteArrayInputStream(bmp));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(input_image, "PNG", out);
            bmpImageView.setImage(new Image(new ByteArrayInputStream(out.toByteArray()), 200, 200, true, true));
        } catch (Exception e) {
            log.warn("pb refreshing bmpImageView with bmpAsBase64", e);
        }
    }

}
