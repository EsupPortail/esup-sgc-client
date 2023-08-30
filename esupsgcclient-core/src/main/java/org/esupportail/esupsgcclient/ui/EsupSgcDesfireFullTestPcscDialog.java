package org.esupportail.esupsgcclient.ui;

import nfcjlib.core.DESFireEV1;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.smartcardio.CardException;

@Component
public class EsupSgcDesfireFullTestPcscDialog extends EsupSgcTestPcscDialog {

    @Value("${desfireTestKey:0000000000000000}")
    String key;

    @Value("${desfireTestKeyNo:00}")
    String keyNo;

    @Value("${desfireTestKeyType:DES}")
    String keyType;


    @Override
    protected void testApdu() throws CardException {
        DESFireEV1 desfire = new DESFireEV1();
        desfire.connect();
       if(DESFireEV1.RawAuthentication.runAll(desfire, pcscUsbService.hexStringToByteArray(key),  pcscUsbService.hexStringToByteArray(keyNo)[0], DESFireEV1.KeyType.valueOf(keyType)) == null) {
           throw new CardException("Authentification DES with blank Key failed");
       };
    }

}
