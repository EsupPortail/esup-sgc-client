package org.esupportail.esupsgcclient;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleStringProperty;
import javafx.beans.value.ObservableBooleanValue;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;

@Component
public class AppSession {

    public enum READY_CONDITION {

        webcam ("Caméra"),
        auth("Authentification esup-nfc-tag"),
        nfc ("Lecteur USB NFC"),
        nfc_desfire ("Salle de badgegage de type desfire (encodage)"),
        printer ("Imprimante à carte");

        private String name;

        READY_CONDITION(String name) {
            this.name = name;
        }

        public String toString() {
            return name;
        }
    }

    String numeroId = "";

    String sgcAuthToken = "";

    String eppnInit;

    SimpleStringProperty authType = new SimpleStringProperty();

    SimpleBooleanProperty webcamReady = new SimpleBooleanProperty(false);

    SimpleBooleanProperty nfcReady = new SimpleBooleanProperty();

    SimpleBooleanProperty authReady = new SimpleBooleanProperty();

    SimpleBooleanProperty printerReady = new SimpleBooleanProperty();

    SimpleBooleanProperty taskIsRunning = new SimpleBooleanProperty();

    public Map<READY_CONDITION, ObservableBooleanValue> getReadyConditions() {
        return new HashMap<READY_CONDITION, ObservableBooleanValue>() {{
            put(READY_CONDITION.webcam, webcamReady);
            put(READY_CONDITION.auth, authReady);
            put(READY_CONDITION.nfc, nfcReady);
            put(READY_CONDITION.nfc_desfire, authType.isEqualTo("DESFIRE"));
            put(READY_CONDITION.printer, printerReady);
            }};
    }

    public String getNumeroId() {
        return numeroId;
    }

    public void setNumeroId(String numeroId) {
        this.numeroId = numeroId;
    }

    public String getSgcAuthToken() {
        return sgcAuthToken;
    }

    public void setSgcAuthToken(String sgcAuthToken) {
        this.sgcAuthToken = sgcAuthToken;
    }

    public String getEppnInit() {
        return eppnInit;
    }

    public void setEppnInit(String eppnInit) {
        this.eppnInit = eppnInit;
    }

    public String getAuthType() {
        return authType.get();
    }

    public SimpleStringProperty authTypeProperty() {
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType.set(authType);
    }

    public boolean isWebcamReady() {
        return webcamReady.get();
    }

    public SimpleBooleanProperty webcamReadyProperty() {
        return webcamReady;
    }

    public void setWebcamReady(boolean webcamReady) {
        this.webcamReady.set(webcamReady);
    }

    public boolean isNfcReady() {
        return nfcReady.get();
    }

    public SimpleBooleanProperty nfcReadyProperty() {
        return nfcReady;
    }

    public void setNfcReady(boolean nfcReady) {
        this.nfcReady.set(nfcReady);
    }

    public boolean isAuthReady() {
        return authReady.get();
    }

    public SimpleBooleanProperty authReadyProperty() {
        return authReady;
    }

    public void setAuthReady(boolean authReady) {
        this.authReady.set(authReady);
    }

    public boolean isPrinterReady() {
        return printerReady.get();
    }

    public SimpleBooleanProperty printerReadyProperty() {
        return printerReady;
    }

    public void setPrinterReady(boolean printerReady) {
        this.printerReady.set(printerReady);
    }

    public boolean isTaskIsRunning() {
        return taskIsRunning.get();
    }

    public SimpleBooleanProperty taskIsRunningProperty() {
        return taskIsRunning;
    }

    public void setTaskIsRunning(boolean taskIsRunning) {
        this.taskIsRunning.set(taskIsRunning);
    }

}
