package org.esupportail.esupsgcclient.taskencoding;

import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Map;

public class TaskParamBean {

    final Map<UiStep, TextFlow> uiSteps;

    final ObjectProperty<Image> webcamImageProperty;

    final ImageView bmpColorImageView;

    final ImageView bmpBlackImageView;

    public TaskParamBean(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> imageProperty,
                         ImageView bmpColorImageView, ImageView bmpBlackImageView) {
        this.uiSteps = uiSteps;
        this.webcamImageProperty = imageProperty;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
    }

}
