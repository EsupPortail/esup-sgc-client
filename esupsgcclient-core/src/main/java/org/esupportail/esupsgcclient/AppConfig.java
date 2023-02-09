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

    @Value("${printerDeviceName}")
    String printerDeviceName = "Evolis Primacy 2";

    @Value("${printerEvolisSet}")
    String printerEvolisSet = "GRibbonType=RC_YMCKO;Duplex=NONE";

   @Value("${printerZebraEncoderType:}")
   String  printerZebraEncoderType;

    public String getEsupNfcTagServerUrl() {
        return esupNfcTagServerUrl;
    }

    public String getEsupSgcUrl() {
        return esupSgcUrl;
    }

    public boolean isEncodeCnous() {
        return encodeCnous;
    }

    public String getPrinterEvolisIp() {
        return printerEvolisIp;
    }

    public int getPrinterEvolisPort() {
        return printerEvolisPort;
    }

    public String getPrinterDeviceName() {
        return printerDeviceName;
    }

    public String getPrinterEvolisSet() {
        return printerEvolisSet;
    }

    public String getPrinterZebraEncoderType() {
        return printerZebraEncoderType;
    }
}
