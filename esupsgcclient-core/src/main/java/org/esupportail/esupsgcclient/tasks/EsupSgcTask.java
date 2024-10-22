package org.esupportail.esupsgcclient.tasks;

import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.esupportail.esupsgcclient.utils.Utils;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.List;
import java.util.Map;

public abstract class EsupSgcTask extends Task<String> {

    private final static Logger log = Logger.getLogger(EsupSgcTask.class);

    private final static int PROGRESS_STEP_LENGTH = 200;

    Map<UiStep, TextFlow> uiSteps;

    public ObjectProperty<Image> webcamImageProperty;

    protected ImageView bmpColorImageView;

    protected ImageView bmpBlackImageView;

    protected ImageView bmpBackImageView;

    UiStep lastUiStepSuccess = null;

    public EsupSgcTask(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty, ImageView bmpColorImageView, ImageView bmpBlackImageView, ImageView bmpBackImageView) {
        this.uiSteps = uiSteps;
        this.webcamImageProperty = webcamImageProperty;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
        this.bmpBackImageView = bmpBackImageView;
    }

    protected abstract List<UiStep> getUiStepsList();

    protected void setUiStepFailed(UiStep uiStep, Throwable exception) {
        Utils.jfxRunLaterIfNeeded(() -> {
            uiSteps.get(uiStep).getStyleClass().clear();
            uiSteps.get(uiStep).getStyleClass().add("alert-danger");
         });
    }

    protected void setUiStepRunning() {
        Utils.jfxRunLaterIfNeeded(() -> {
            resetUiSteps();
            for (UiStep step : getUiStepsList()) {
                uiSteps.get(step).setVisible(true);
            }
        });
    }

    void resetUiSteps() {
        Utils.jfxRunLaterIfNeeded(() -> {
            for(UiStep step : uiSteps.keySet()) {
                uiSteps.get(step).setVisible(false);
                uiSteps.get(step).getStyleClass().clear();
                uiSteps.get(step).getStyleClass().add("alert-info");
            }
            lastUiStepSuccess = null;
        });
    }

   public void  updateTitle4thisTask(String title) {
        updateTitle(title);
        // just to let time to update title / logtextarea in UI
        Utils.sleep(5);
    }

	protected void setUiStepSuccess(UiStep uiStep) {
        Utils.jfxRunLaterIfNeeded(() -> {
            if (uiStep == null) {
                updateProgress(0, getUiStepsList().size() * PROGRESS_STEP_LENGTH);
                updateTitle("En attente ...");
                TextFlow uiStepTextFlow = (TextFlow) uiSteps.get(getUiStepsList().get(0));
                uiStepTextFlow.getStyleClass().clear();
                uiStepTextFlow.getStyleClass().add("alert-warning");
            } else {
                TextFlow uiStepTextFlow = (TextFlow) uiSteps.get(uiStep);
                uiStepTextFlow.getStyleClass().clear();
                uiStepTextFlow.getStyleClass().add("alert-success");
                updateProgress((getUiStepsList().indexOf(uiStep) + 1) * PROGRESS_STEP_LENGTH, getUiStepsList().size() * PROGRESS_STEP_LENGTH);
                if (getUiStepsList().indexOf(uiStep) + 1 < getUiStepsList().size()) {
                    UiStep newtUiStep = (UiStep) getUiStepsList().get(getUiStepsList().indexOf(uiStep) + 1);
                    updateTitle(newtUiStep.toString());
                    uiStepTextFlow = (TextFlow) uiSteps.get(newtUiStep);
                    uiStepTextFlow.getStyleClass().clear();
                    uiStepTextFlow.getStyleClass().add("alert-warning");
                }
            }
            lastUiStepSuccess = uiStep;
        });
	}

    public void updateProgressStep() {
       Utils.jfxRunLaterIfNeeded(() -> {
           updateProgress(getProgress() * getUiStepsList().size() * PROGRESS_STEP_LENGTH + 1, getUiStepsList().size() * PROGRESS_STEP_LENGTH);
       });
    }

    protected void setCurrentUiStepFailed(Throwable exception) {
        Utils.jfxRunLaterIfNeeded(() -> {
            UiStep currentUiStep = null;
            if (lastUiStepSuccess == null) {
                currentUiStep = getUiStepsList().get(0);
            } else if (lastUiStepSuccess != null && getUiStepsList().indexOf(lastUiStepSuccess) + 1 < getUiStepsList().size()) {
                currentUiStep = (UiStep) getUiStepsList().get(getUiStepsList().indexOf(lastUiStepSuccess) + 1);
            }
            if (currentUiStep != null) {
                setUiStepFailed(currentUiStep, exception);
            }
        });
    }

    protected void updateBmpUi(String bmpAsBase64, ImageView bmpImageView) {
        try {
            byte[] bmp = Base64.getDecoder().decode(bmpAsBase64.getBytes());
            BufferedImage input_image = ImageIO.read(new ByteArrayInputStream(bmp));
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            ImageIO.write(input_image, "PNG", out);
            Utils.jfxRunLaterIfNeeded(() -> {
                bmpImageView.setImage(new Image(new ByteArrayInputStream(out.toByteArray()), input_image.getWidth(), input_image.getHeight(), true, true));
            });
        } catch (Exception e) {
            log.warn("pb refreshing bmpImageView with bmpAsBase64", e);
        }
    }


    protected void resetBmpUi() {
        Utils.jfxRunLaterIfNeeded(() -> {
            bmpColorImageView.setImage(null);
            bmpBlackImageView.setImage(null);
            bmpBackImageView.setImage(null);
        });
    }


}
