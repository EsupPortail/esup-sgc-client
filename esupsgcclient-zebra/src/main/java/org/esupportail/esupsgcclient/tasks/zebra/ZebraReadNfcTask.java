package org.esupportail.esupsgcclient.tasks.zebra;

import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.pcsc.NfcResultBean;
import org.esupportail.esupsgcclient.service.printer.zebra.ZebraPrinterService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

public class ZebraReadNfcTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(ZebraReadNfcTask.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.printer_nfc,
            UiStep.encode});

    ZebraPrinterService zebraPrinterService;
    EncodingService encodingService;
    public ZebraReadNfcTask(Map<UiStep, TextFlow> uiSteps, ZebraPrinterService zebraPrinterService, EncodingService encodingService) {
        super(uiSteps);
        this.zebraPrinterService = zebraPrinterService;
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
            zebraPrinterService.launchEncoding();
            setUiStepSuccess(UiStep.printer_nfc);
            NfcResultBean nfcResultBean = encodingService.encode(this);
            setUiStepSuccess(UiStep.encode);
            zebraPrinterService.eject();
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            zebraPrinterService.cancelJobs();
            //zebraPrinterService.reset();
            updateTitle("Carte rejetée");
            throw new RuntimeException("Exception on  ZebraReadNfcTask : " + e.getMessage(), e);
        }
		return null;
	}

}
