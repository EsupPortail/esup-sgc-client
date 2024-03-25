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

   @Value("${printerZebraEncoderType:}")
   String  printerZebraEncoderType;

    @Value("${printerZebraHackZxpNfcPower:false}")
   Boolean printerZebraHackZxpNfcPower;

    public String getEsupNfcTagServerUrl() {
        return esupNfcTagServerUrl;
    }

    public String getEsupSgcUrl() {
        return esupSgcUrl;
    }

    public boolean isEncodeCnous() {
        return encodeCnous;
    }

    public String getPrinterZebraEncoderType() {
        return printerZebraEncoderType;
    }

    public Boolean getPrinterZebraHackZxpNfcPower() {
        return printerZebraHackZxpNfcPower;
    }

    public void setPrinterZebraHackZxpNfcPower(Boolean printerZebraHackZxpNfcPower) {
        this.printerZebraHackZxpNfcPower = printerZebraHackZxpNfcPower;
    }
}
