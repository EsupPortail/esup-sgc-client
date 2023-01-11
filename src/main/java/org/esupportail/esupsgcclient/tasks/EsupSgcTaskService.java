package org.esupportail.esupsgcclient.tasks;

import com.beust.jcommander.Strings;
import javafx.beans.Observable;
import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.value.ObservableBooleanValue;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.image.ImageView;
import javafx.scene.text.TextFlow;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.ui.UiStep;

import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public abstract class EsupSgcTaskService extends javafx.concurrent.Service<String> {

    private final static Logger log = Logger.getLogger(EsupSgcTaskService.class);

    protected Map<UiStep, TextFlow> uiSteps;
    protected ImageView webcamImageView;
    protected ImageView bmpColorImageView;
    protected ImageView bmpBlackImageView;
    protected BooleanBinding readyToRunProperty;


    public void setup(Map<UiStep, TextFlow> uiSteps, ImageView webcamImageView, ImageView bmpColorImageView, ImageView bmpBlackImageView) {
        this.uiSteps = uiSteps;
        this.webcamImageView = webcamImageView;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
    }

    public abstract AppSession getAppSession();

    public abstract AppSession.READY_CONDITION[] readyToRunConditions();

    public abstract String getLabel();

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
        log.warn(pbs);
        return Strings.join("\n", pbs) + "\n";
    }

}
