package org.esupportail.esupsgcclient;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ObservableValue;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;

@Component
public class AppSession {

    String numeroId = "";
    String sgcAuthToken = "";
    String eppnInit;

    String authType;

    SimpleBooleanProperty webcamReady = new SimpleBooleanProperty(false);

    SimpleBooleanProperty nfcReady = new SimpleBooleanProperty();

    SimpleBooleanProperty authReady = new SimpleBooleanProperty();

    SimpleBooleanProperty printerReady = new SimpleBooleanProperty();

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
        return authType;
    }

    public void setAuthType(String authType) {
        this.authType = authType;
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

    public ObservableValue<Boolean> getNfcReady() {
        return nfcReady;
    }

    public ObservableValue<Boolean> getAuthReady() {
        return authReady;
    }

    public ObservableValue<Boolean> getWebcamReady() {
        return webcamReady;
    }


    public ObservableValue<Boolean> getPrinterReady() {
        return printerReady;
    }
}
