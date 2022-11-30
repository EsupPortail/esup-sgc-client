package org.esupportail.esupsgcclient.tasks;

import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.scene.image.Image;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

@Component
public class QrCodeTaskService extends Service<String> {

	private final static Logger log = Logger.getLogger(QrCodeTaskService.class);

	Map<UiStep, TextFlow> uiSteps;
	ObjectProperty<Image> webcamImageProperty;

	@Resource
	EncodingService encodingService;

	public void setup(Map<UiStep, TextFlow> uiSteps, ObjectProperty<Image> webcamImageProperty) {
		this.uiSteps = uiSteps;
		this.webcamImageProperty = webcamImageProperty;
	}

	@Override
	protected Task<String> createTask() {
		return new QrCodeTask(uiSteps, webcamImageProperty, encodingService);
	}

}
