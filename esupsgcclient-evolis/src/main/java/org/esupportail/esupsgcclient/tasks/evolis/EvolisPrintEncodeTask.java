package org.esupportail.esupsgcclient.tasks.evolis;

import jakarta.annotation.Resource;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.sgc.EsupSgcRestClientService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class EvolisPrintEncodeTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(EvolisPrintEncodeTaskService.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.long_poll,
            UiStep.bmp_black,
            UiStep.bmp_color,
            UiStep.printer_color,
            UiStep.printer_black,
            UiStep.printer_overlay,
            UiStep.printer_print,
            UiStep.printer_nfc,
            UiStep.encode
    });

    ImageView bmpColorImageView;

    ImageView bmpBlackImageView;
    @Resource
    EsupSgcRestClientService esupSgcRestClientService;

    @Resource
    EncodingService encodingService;

    @Resource
    EvolisPrinterService evolisPrinterService;

    public EvolisPrintEncodeTask(Map<UiStep, TextFlow> uiSteps, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
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
            String evolisPrinterStatus = evolisPrinterService.getPrinterStatus().getResult();
            while(!evolisPrinterStatus.contains("PRINTER_READY")) {
                updateTitle(String.format("Status de l'imprimante evolis non prête (%s) - en attente ...", evolisPrinterStatus));
                Utils.sleep(5000);
                evolisPrinterStatus = evolisPrinterService.getPrinterStatus().getResult();
            }
            evolisPrinterService.startSequence();
            evolisPrinterService.noEject();
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
            evolisPrinterService.printEnd();
            evolisPrinterService.insertCardToContactLessStation(this);
            evolisPrinterStatus = evolisPrinterService.getPrinterStatus().getResult();
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
