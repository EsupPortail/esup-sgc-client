package org.esupportail.esupsgcclient.tasks.evolis;

import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.pcsc.NfcResultBean;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class EvolisReadNfcTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(EvolisEncodePrintTaskService.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.printer_nfc,
            UiStep.encode});

    EvolisPrinterService evolisPrinterService;
    EncodingService encodingService;
    public EvolisReadNfcTask(Map<UiStep, TextFlow> uiSteps, EvolisPrinterService evolisPrinterService, EncodingService encodingService) {
        super(uiSteps);
        this.evolisPrinterService = evolisPrinterService;
        this.encodingService = encodingService;
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
            evolisPrinterService.insertCardToContactLessStation(this);
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
