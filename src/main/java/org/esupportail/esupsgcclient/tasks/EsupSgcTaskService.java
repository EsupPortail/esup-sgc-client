package org.esupportail.esupsgcclient.tasks;

import com.beust.jcommander.Strings;
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

    public abstract AppSession getAppSession();

    public abstract AppSession.READY_CONDITION[] readyToRunConditions();

    public BooleanBinding readyToRunProperty() {
        Map<AppSession.READY_CONDITION, ObservableBooleanValue> readyToRunConditionsMap = getAppSession().getReadyConditions().entrySet()
                .stream()
                .filter(a-> Arrays.asList(readyToRunConditions()).contains(a.getKey()))
                .collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));
        ObservableList<ObservableBooleanValue> readyToRunPropertiesList = FXCollections.observableArrayList(readyToRunConditionsMap.values());
        return Bindings.createBooleanBinding(() -> readyToRunPropertiesList.stream().allMatch(observableBooleanValue -> observableBooleanValue.get()), readyToRunPropertiesList);
    }

    public String readyToRunPropertyDisplayProblem() {
        Map<AppSession.READY_CONDITION, ObservableBooleanValue> readyToRunConditionsFailedMap = getAppSession().getReadyConditions().entrySet()
                .stream()
                .filter(a-> Arrays.asList(readyToRunConditions()).contains(a.getKey()) && !a.getValue().get())
                .collect(Collectors.toMap(e->e.getKey(),e->e.getValue()));
        List<String> pbs = readyToRunConditionsFailedMap.keySet().stream().map(k -> k + " : KO").collect(Collectors.toList());
        log.warn(pbs);
        return Strings.join("\n", pbs) + "\n";
    }

}
