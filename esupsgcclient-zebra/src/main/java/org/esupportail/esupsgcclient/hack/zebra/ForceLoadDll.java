package org.esupportail.esupsgcclient.hack.zebra;

import com.zebra.sdk.comm.internal.NativeUsbAdapter;
import org.apache.commons.io.FileUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.service.printer.zebra.ZebraPrinterService;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Date;

/*
 Hack to force load the Zebra native library.
 We call NattiveUsbAdapter.class to load it and then we set NativeUsbAdapter.isDriverLoaded to true.
 */
public class ForceLoadDll {

    private final static Logger log = LoggerFactory.getLogger(ForceLoadDll.class);

    public static void loadDll() {
        if(System.getProperty("os.name").toLowerCase().contains("windows")) {
            log.info("Loading Zebra native library - NativeUsbAdapter.isDriverLoaded = " + NativeUsbAdapter.isDriverLoaded);
            try {
                String dllPath = System.getProperty("sun.arch.data.model").contains("32") ? "ZebraNativeUsbAdapter_32.dll" : "ZebraNativeUsbAdapter_64.dll";
                log.info("Loading Zebra native library from " + dllPath);
                InputStream in = ClassLoader.getSystemResourceAsStream(dllPath);
                File outFile = new File(System.getProperty("java.io.tmpdir") + "ZebraNativeUsbAdapter-" + new Date().getTime() + ".dll");
                outFile.deleteOnExit();
                FileUtils.copyInputStreamToFile(in, outFile);
                log.info("Copying " + dllPath + " to " + outFile.getAbsolutePath());
                System.load(outFile.getAbsolutePath());
                log.info("Zebra native library loaded");
                NativeUsbAdapter.isDriverLoaded = true;
            } catch (IOException e) {
                throw new RuntimeException(e);
            }
        }

    }

}
