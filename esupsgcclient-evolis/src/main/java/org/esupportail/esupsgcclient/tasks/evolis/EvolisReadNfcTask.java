package org.esupportail.esupsgcclient.tasks.evolis;

import javax.annotation.Resource;

import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.pcsc.NfcResultBean;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
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
public class EvolisReadNfcTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(EvolisReadNfcTask.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.printer_nfc,
            UiStep.encode});

    @Resource
    EvolisPrinterService evolisPrinterService;

    @Resource
    EncodingService encodingService;

    public EvolisReadNfcTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
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
            // evolisPrinterService.setupCardToContactLessStation();
            evolisPrinterService.insertCardToContactLessStation(this);
            String evolisPrinterStatus = evolisPrinterService.getPrinterStatus().getResult();
            while(!evolisPrinterStatus.contains("ENCODING_RUNNING")) {
                updateTitle(String.format("en attente d'une carte ...", evolisPrinterStatus));
                Utils.sleep(100);
                evolisPrinterStatus = evolisPrinterService.getPrinterStatus().getResult();
            }
            setUiStepSuccess(UiStep.printer_nfc);
            encodingService.pcscConnection(this);
            encodingService.waitForCardPresent(5000);
            setUiStepSuccess(UiStep.printer_nfc);
            NfcResultBean nfcResultBean = encodingService.encode(this);
            if(!nfcResultBean.inError()) {
                setUiStepSuccess(UiStep.encode);
                evolisPrinterService.eject();
            } else {
                setUiStepFailed(UiStep.encode, null);
                evolisPrinterService.reject();
                updateTitle("Carte rejetée");
            }
            if(!encodingService.waitForCardAbsent(5000)) {
                throw new RuntimeException("La carte n'a pas été éjectée et est toujours sur le lecteur NFC de l'imprimante après 5 secondes ?");
            }
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            evolisPrinterService.reject();
            updateTitle("Carte rejetée");
            throw new RuntimeException("Exception on  EvolisReadNfcTask : " + e.getMessage(), e);
        }
		return null;
	}

}
