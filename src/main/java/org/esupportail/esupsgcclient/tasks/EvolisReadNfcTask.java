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

public class EvolisReadNfcTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(EvolisTaskService.class);

    EvolisPrinterService evolisPrinterService;
    EncodingService encodingService;
    public EvolisReadNfcTask(Map<UiStep, TextFlow> uiSteps, EvolisPrinterService evolisPrinterService, EncodingService encodingService) {
        super(uiSteps);
        this.evolisPrinterService = evolisPrinterService;
        this.encodingService = encodingService;
    }

    @Override
    List<UiStep> getUiStepsList() {
        return Arrays.asList(new UiStep[]{
                UiStep.printer_nfc,
                UiStep.encode});
    }

    @Override
    protected String call() throws Exception {
        try {
            setUiStepRunning();
            setUiStepSuccess(null);
            evolisPrinterService.insertCardToContactLessStation(this);
            setUiStepSuccess(UiStep.printer_nfc);
            encodingService.encode(this);
            setUiStepSuccess(UiStep.encode);
            evolisPrinterService.eject();
            if(!encodingService.waitForCardAbsent(5000)) {
                throw new RuntimeException("La carte n'a pas été éjectée et est toujours sur le lecteur NFC de l'imprimante après 5 secondes ?");
            }
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            evolisPrinterService.reject();
            throw new RuntimeException("Exception on  EvolisReadNfcTask : " + e.getMessage(), e);
        }
		return null;
	}

}
