package org.esupportail.esupsgcclient.tasks;

import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisException;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisResponse;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcLongPollService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public class EvolisTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(EvolisTaskService.class);

    ImageView bmpColorImageView;

    ImageView bmpBlackImageView;
    EsupSgcLongPollService esupSgcLongPollService;
    EvolisPrinterService evolisPrinterService;
    EncodingService encodingService;
    public EvolisTask(Map<UiStep, TextFlow> uiSteps, ImageView bmpColorImageView, ImageView bmpBlackImageView,
                      EsupSgcLongPollService esupSgcLongPollService, EvolisPrinterService evolisPrinterService, EncodingService encodingService) {
        super(uiSteps);
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
        this.esupSgcLongPollService = esupSgcLongPollService;
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
                UiStep.encode,
                UiStep.printer_print});
    }

    @Override
    protected String call() throws Exception {
        try {
            setUiStepRunning();
            setUiStepSuccess(null);
            String qrcode = esupSgcLongPollService.getQrCode(this);
            setUiStepRunning();
            setUiStepSuccess(UiStep.long_poll);
            String bmpBlackAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.black);
            updateBmpUi(bmpBlackAsBase64, bmpBlackImageView);
            setUiStepSuccess(UiStep.bmp_black);
            String bmpColorAsBase64 = encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.color);
            updateBmpUi(bmpColorAsBase64, bmpColorImageView);
            setUiStepSuccess(UiStep.bmp_color);
            EvolisResponse resp = evolisPrinterService.insertCardToContactLessStation();
            setUiStepSuccess(UiStep.printer_nfc);
            encodingService.encode(qrcode);
            setUiStepSuccess(UiStep.encode);
            evolisPrinterService.printBegin();
            evolisPrinterService.printSet();
            evolisPrinterService.printFrontColorBmp(bmpColorAsBase64);
            setUiStepSuccess(UiStep.printer_color);
            evolisPrinterService.printFrontBlackBmp(bmpBlackAsBase64);
            setUiStepSuccess(UiStep.printer_black);
            evolisPrinterService.printFrontVarnish(bmpBlackAsBase64);
            setUiStepSuccess(UiStep.printer_overlay);
            evolisPrinterService.print();
            setUiStepSuccess(UiStep.printer_print);
        } catch (Exception e) {
            updateTitle(e.getMessage());
            log.error("Exception on  EvolisTaskService : " + e.getMessage(), e);
        } finally {
            //evolisPrinterService.printEnd();
            resetBmpUi();
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
