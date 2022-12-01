package org.esupportail.esupsgcclient;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.PropertySource;

@Configuration
@PropertySource("classpath:esupsgcclient.properties")
public class AppConfig {

    @Value("${esupNfcTagServerUrl}")
    String esupNfcTagServerUrl;

    @Value("${esupSgcUrl}")
    String esupSgcUrl;

    @Value("${encodeCnous}")
    boolean encodeCnous = false;
    @Value("${localStorageDir}")
    String localStorageDir;
    @Value("${printerEvolisIp}")
    String printerEvolisIp = "127.0.0.1";
    @Value("${printerEvolisPort}")
    int printerEvolisPort = 18000;

    @Value("${printerEvolisSet}")
    String printerEvolisSet = "GRibbonType=RC_YMCKO;Duplex=NONE";

    public String getEsupNfcTagServerUrl() {
        return esupNfcTagServerUrl;
    }

    public void setEsupNfcTagServerUrl(String esupNfcTagServerUrl) {
        this.esupNfcTagServerUrl = esupNfcTagServerUrl;
    }

    public String getEsupSgcUrl() {
        return esupSgcUrl;
    }

    public void setEsupSgcUrl(String esupSgcUrl) {
        this.esupSgcUrl = esupSgcUrl;
    }

    public boolean isEncodeCnous() {
        return encodeCnous;
    }

    public void setEncodeCnous(boolean encodeCnous) {
        this.encodeCnous = encodeCnous;
    }

    public String getLocalStorageDir() {
        return localStorageDir;
    }

    public void setLocalStorageDir(String localStorageDir) {
        this.localStorageDir = localStorageDir;
    }

    public String getPrinterEvolisIp() {
        return printerEvolisIp;
    }

    public void setPrinterEvolisIp(String printerEvolisIp) {
        this.printerEvolisIp = printerEvolisIp;
    }

    public int getPrinterEvolisPort() {
        return printerEvolisPort;
    }

    public void setPrinterEvolisPort(int printerEvolisPort) {
        this.printerEvolisPort = printerEvolisPort;
    }

    public String getPrinterEvolisSet() {
        return printerEvolisSet;
    }

    public void setPrinterEvolisSet(String printerEvolisSet) {
        this.printerEvolisSet = printerEvolisSet;
    }
}
