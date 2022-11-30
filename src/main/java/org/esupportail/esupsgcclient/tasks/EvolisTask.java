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
import java.util.Base64;
import java.util.Map;

@Service
public class EvolisTask extends Task<String> {

    private final static Logger log = Logger.getLogger(EvolisTaskService.class);
    EvolisTaskService evolisTaskService;

    public EvolisTask(EvolisTaskService evolisTaskService) {
        this.evolisTaskService = evolisTaskService;
    }

    @Override
    protected String call() throws Exception {
        try {
            setUiStepSuccess(null);
            String qrcode = evolisTaskService.esupSgcLongPollService.getQrCode();
            setUiStepSuccess(UiStep.long_poll);
            String bmpBlackAsBase64 = evolisTaskService.encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.black);
            updateBmpUi(bmpBlackAsBase64, evolisTaskService.bmpBlackImageView);
            setUiStepSuccess(UiStep.bmp_black);
            String bmpColorAsBase64 = evolisTaskService.encodingService.getBmpAsBase64(qrcode, EncodingService.BmpType.color);
            updateBmpUi(bmpColorAsBase64, evolisTaskService.bmpColorImageView);
            setUiStepSuccess(UiStep.bmp_color);
            EvolisResponse resp = evolisTaskService.evolisPrinterService.insertCardToContactLessStation();
            setUiStepSuccess(UiStep.printer_nfc);
            evolisTaskService.encodingService.encode(qrcode);
            setUiStepSuccess(UiStep.encode);
            evolisTaskService.evolisPrinterService.printBegin();
            evolisTaskService.evolisPrinterService.printSet();
            evolisTaskService.evolisPrinterService.printFrontColorBmp(bmpColorAsBase64);
            setUiStepSuccess(UiStep.printer_color);
            evolisTaskService.evolisPrinterService.printFrontBlackBmp(bmpBlackAsBase64);
            setUiStepSuccess(UiStep.printer_black);
            evolisTaskService.evolisPrinterService.printFrontVarnish(bmpBlackAsBase64);
            setUiStepSuccess(UiStep.printer_overlay);
            evolisTaskService.evolisPrinterService.print();
            setUiStepSuccess(UiStep.printer_print);
        } catch (EvolisException evolisException) {
            updateTitle(evolisException.getMessage());
            log.error("Exception with evolis : " + evolisException.getMessage(), evolisException);
        } finally {
            evolisTaskService.evolisPrinterService.printEnd();
            resetBmpUi();
        }
		return null;
	}

    private void resetBmpUi() {
        evolisTaskService.bmpColorImageView.setImage(null);
        evolisTaskService.bmpBlackImageView.setImage(null);
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

	public void setUiStepSuccess(UiStep uiStep) {
        if(uiStep == null) {
            updateProgress(0, evolisTaskService.uiStepsList.size());
            updateTitle("En attente ...");
        } else {
            evolisTaskService.uiSteps.get(uiStep).getStyleClass().clear();
            evolisTaskService.uiSteps.get(uiStep).getStyleClass().add("alert-success");
            updateProgress(evolisTaskService.uiStepsList.indexOf(uiStep), evolisTaskService.uiStepsList.size());
            if(evolisTaskService.uiStepsList.indexOf(uiStep)+1<evolisTaskService.uiStepsList.size()) {
                UiStep newtUiStep = evolisTaskService.uiStepsList.get(evolisTaskService.uiStepsList.indexOf(uiStep) + 1);
                updateTitle(newtUiStep.toString());
            }
        }
	}

}
