package org.esupportail.esupsgcclient.tasks.simple;

import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ReadNfcTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(ReadNfcTask.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{UiStep.encode});

    EncodingService encodingService;

    public ReadNfcTask(Map<UiStep, TextFlow> uiSteps, EncodingService encodingService) {
        super(uiSteps);
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
            encodingService.encode(this);
            setUiStepSuccess(UiStep.encode);
            updateTitle4thisTask("Badgeage OK");
            updateTitle4thisTask("Merci de retirer cette carte");
            while (!encodingService.waitForCardAbsent(1000)) {
                if(isCancelled()) {
                    return null;
                }
            }
            updateTitle4thisTask("Carte retirée");
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            throw new RuntimeException("Exception on  EvolisReadNfcTask : " + e.getMessage(), e);
        }
		return null;
	}

}
