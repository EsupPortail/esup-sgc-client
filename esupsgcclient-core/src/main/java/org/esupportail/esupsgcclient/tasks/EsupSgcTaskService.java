package org.esupportail.esupsgcclient.tasks;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class EsupSgcTaskService extends javafx.concurrent.Service<String> {

    private final static Logger log = LoggerFactory.getLogger(EsupSgcTaskService.class);

    protected Map<UiStep, TextFlow> uiSteps;
    protected ImageView webcamImageView;
    protected ImageView bmpColorImageView;
    protected ImageView bmpBlackImageView;
    protected ImageView bmpBackImageView;
    protected BooleanBinding readyToRunProperty;


    public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView, ImageView bmpBackImageView) {
        this.uiSteps = uiSteps;
        this.webcamImageView = webcamImageView;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
        this.bmpBackImageView = bmpBackImageView;
    }

    public abstract AppSession getAppSession();

    public abstract AppSession.READY_CONDITION[] readyToRunConditions();

    public abstract String getLabel();

    public abstract List<UiStep> getUiStepsList();

    public BooleanBinding readyToRunProperty() {
        if(readyToRunProperty == null) {
            Map<AppSession.READY_CONDITION, ObservableBooleanValue> readyToRunConditionsMap = getAppSession().getReadyConditions().entrySet()
                    .stream()
                    .filter(a -> Arrays.asList(readyToRunConditions()).contains(a.getKey()))
                    .collect(Collectors.toMap(e -> e.getKey(), e -> e.getValue()));
            ObservableBooleanValue[] dependencies = readyToRunConditionsMap.values().stream().toArray(ObservableBooleanValue[]::new);
            readyToRunProperty = Bindings.createBooleanBinding(() -> readyToRunConditionsMap.values().stream().allMatch(ObservableBooleanValue::get), dependencies);
        }
        return readyToRunProperty;
    }

    public String readyToRunPropertyDisplayProblem() {
        Map<AppSession.READY_CONDITION, ObservableBooleanValue> readyToRunConditionsFailedMap = getAppSession().getReadyConditions().entrySet()
                .stream()
                .filter(a-> Arrays.asList(readyToRunConditions()).contains(a.getKey()) && !a.getValue().get())
                .collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));
        List<String> pbs = readyToRunConditionsFailedMap.keySet().stream().map(k -> " * " + k + " : KO").collect(Collectors.toList());
        log.warn(StringUtils.join(pbs, ","));
        return String.join("\n", pbs) + "\n";
    }

}
