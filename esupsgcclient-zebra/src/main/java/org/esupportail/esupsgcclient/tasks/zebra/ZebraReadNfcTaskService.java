package org.esupportail.esupsgcclient.tasks.zebra;

import javax.annotation.Resource;
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

import java.util.List;
import java.util.Map;

@Service
public class ZebraReadNfcTaskService extends EsupSgcTaskService {

	private final static Logger log = Logger.getLogger(ZebraReadNfcTaskService.class);

	static final String BADGEAGE_EN_SERIE_VIA_ZEBRA = "Badgeage en série via Imprimante Zebra";

	@Resource
	private ApplicationContext ctx;

	@Resource
	AppSession appSession;

	@Override
	public List<UiStep> getUiStepsList() {
		return ZebraReadNfcTask.UI_STEPS_LIST;
	}

	@Override
	protected Task<String> createTask() {
		return ctx.getBean(ZebraReadNfcTask.class, uiSteps, webcamImageView.imageProperty(),  bmpColorImageView, bmpBlackImageView);
	}

	@Override
	public AppSession getAppSession() {
		return appSession;
	}

	@Override
	public String getLabel() {
		return BADGEAGE_EN_SERIE_VIA_ZEBRA;
	}

	@Override
	public READY_CONDITION[] readyToRunConditions() {
		return new  READY_CONDITION[] { READY_CONDITION.auth, READY_CONDITION.nfc, READY_CONDITION.printer};
	}

	@Override
	public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView, ImageView bmpBackImageView) {
		super.setup(uiSteps, webcamImageView, bmpColorImageView, bmpBlackImageView, bmpBackImageView);
		webcamImageView.setVisible(false);
		bmpColorImageView.setVisible(false);
		bmpBlackImageView.setVisible(false);
		bmpBackImageView.setVisible(false);
	}
}
