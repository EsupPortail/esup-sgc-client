package org.esupportail.esupsgcclient.tasks.zebra;

import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.pcsc.NfcResultBean;
import org.esupportail.esupsgcclient.service.printer.zebra.ZebraPrinterService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
@Scope("prototype")
public class ZebraReadNfcTask extends EsupSgcTask {

    private final static Logger log = Logger.getLogger(ZebraReadNfcTask.class);

    final static List<UiStep> UI_STEPS_LIST =  Arrays.asList(new UiStep[]{
            UiStep.printer_nfc,
            UiStep.encode});

    @Resource
    ZebraPrinterService zebraPrinterService;

    @Resource
    EncodingService encodingService;

    public ZebraReadNfcTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
        super(uiSteps, webcamImageProperty, bmpColorImageView, bmpBlackImageView);
    }

    @Override
    protected List<UiStep> getUiStepsList() {
        return UI_STEPS_LIST;
    }

    @Override
    protected String call() throws Exception {
        try {
            Utils.sleep(500);
            setUiStepRunning();
            setUiStepSuccess(null);
            zebraPrinterService.launchEncoding();
            setUiStepSuccess(UiStep.printer_nfc);
            encodingService.encode(this);
            setUiStepSuccess(UiStep.encode);
            zebraPrinterService.eject();
        } catch (Exception e) {
            setCurrentUiStepFailed(e);
            zebraPrinterService.cancelJobs();
            //zebraPrinterService.reset();
            updateTitle("Carte rejetée");
            throw new RuntimeException("Exception on  ZebraReadNfcTask : " + e.getMessage(), e);
        } finally {
            zebraPrinterService.cancelJob();
        }
        updateTitle4thisTask("ZebraReadNfcTask OK");
		return null;
	}

}
