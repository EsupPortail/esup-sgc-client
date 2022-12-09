package org.esupportail.esupsgcclient.tasks;

import javafx.beans.binding.BooleanBinding;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Map;

public abstract class EsupSgcTaskService extends javafx.concurrent.Service<String> {

    Map<UiStep, TextFlow> uiSteps;
    ImageView webcamImageView;
    ImageView bmpColorImageView;
    ImageView bmpBlackImageView;

    public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
        this.uiSteps = uiSteps;
        this.webcamImageView = webcamImageView;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
    }

    public abstract BooleanBinding readyToRunProperty();
}
