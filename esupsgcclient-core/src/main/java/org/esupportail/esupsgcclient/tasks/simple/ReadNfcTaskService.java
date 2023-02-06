package org.esupportail.esupsgcclient.tasks.simple;

import javax.annotation.Resource;
import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.AppSession.READY_CONDITION;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class ReadNfcTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(ReadNfcTaskService.class);

	static final String BADGEAGE_SIMPLE = "Badgeage simple";

	@Resource
	AppSession appSession;

	@Resource
	private ApplicationContext ctx;

	@Override
	public List<UiStep> getUiStepsList() {
		return ReadNfcTask.UI_STEPS_LIST;
	}

	@Override
	protected Task<String> createTask() {
		return ctx.getBean(ReadNfcTask.class, uiSteps);
	}

	@Override
	public AppSession getAppSession() {
		return appSession;
	}

	@Override
	public String getLabel() {
		return BADGEAGE_SIMPLE;
	}

	@Override
	public READY_CONDITION[] readyToRunConditions() {
		return new READY_CONDITION[]{READY_CONDITION.auth, READY_CONDITION.nfc};
	}

	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView);
		webcamImageView.setVisible(false);
		bmpColorImageView.setVisible(false);
		bmpBlackImageView.setVisible(false);
	}

}
