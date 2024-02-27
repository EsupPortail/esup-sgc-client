package org.esupportail.esupsgcclient.tasks.evolis;

import javafx.concurrent.Task;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.AppSession.READY_CONDITION;
import org.esupportail.esupsgcclient.tasks.EsupSgcTaskService;
import org.esupportail.esupsgcclient.ui.UiStep;
import org.springframework.context.ApplicationContext;
import org.springframework.stereotype.Service;

import javax.annotation.Resource;
import java.util.List;
import java.util.Map;

@Service
public class EvolisEncodeTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(EvolisEncodeTaskService.class);

	static final String ROBOT_ENCODAGE_VIA_EVOLIS = "Robot encodage via webcam et imprimante Evolis";

	@Resource
	AppSession appSession;

	@Resource
	private ApplicationContext ctx;

	@Override
	public List<UiStep> getUiStepsList() {
		return EvolisEncodeTask.UI_STEPS_LIST;
	}

	@Override
	protected Task<String> createTask() {
		return ctx.getBean(EvolisEncodeTask.class, uiSteps, webcamImageView.imageProperty(),  bmpColorImageView, bmpBlackImageView);
	}

	@Override
	protected void cancelled() {
		super.cancelled();
	}

	@Override
	public AppSession getAppSession() {
		return appSession;
	}

	@Override
	public String getLabel() {
		return ROBOT_ENCODAGE_VIA_EVOLIS;
	}

	@Override
	public READY_CONDITION[] readyToRunConditions() {
		return new READY_CONDITION[] {READY_CONDITION.auth, READY_CONDITION.nfc, READY_CONDITION.webcam,
				READY_CONDITION.printer, READY_CONDITION.nfc_desfire};
	}


	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView);
		webcamImageView.setVisible(true);
		bmpColorImageView.setVisible(false);
		bmpBlackImageView.setVisible(false);
	}
}
