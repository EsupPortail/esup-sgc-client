package org.esupportail.esupsgcclient.tasks;

import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReadNfcTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(EvolisTaskService.class);

    EncodingService encodingService;

    public ReadNfcTask(Map<UiStep, TextFlow> uiSteps, EncodingService encodingService) {
        super(uiSteps);
        this.encodingService = encodingService;
    }

    @Override
    List<UiStep> getUiStepsList() {
        return Arrays.asList(new UiStep[]{
                UiStep.encode});
    }

    @Override
    protected String call() throws Exception {
        try {
            setUiStepRunning();
            setUiStepSuccess(null);
            encodingService.encode(this);
            setUiStepSuccess(UiStep.encode);
            updateTitle("Badgeage OK");
            while (!encodingService.waitForCardAbsent(1000)) {
                // Utils.sleep(1000); -  sleep non nécessaire : EncodingService.waitForCardAbsent l'intègre
            }
            updateTitle("Carte retirée");
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            throw new RuntimeException("Exception on  EvolisReadNfcTask : " + e.getMessage(), e);
        }
		return null;
	}

}
