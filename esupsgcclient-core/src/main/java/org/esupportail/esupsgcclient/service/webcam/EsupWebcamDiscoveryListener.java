package org.esupportail.esupsgcclient.service.webcam;

import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.EsupSgcClientJfxController;


public class EsupWebcamDiscoveryListener implements WebcamDiscoveryListener {

    final static Logger log = LoggerFactory.getLogger(EsupWebcamDiscoveryListener.class);

    EsupSgcClientJfxController mainController;

    public EsupWebcamDiscoveryListener(EsupSgcClientJfxController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void webcamFound(WebcamDiscoveryEvent webcamDiscoveryEvent) {
        log.info("webcam found");
        mainController.addWebcamMenuItem(webcamDiscoveryEvent.getWebcam().getName());
    }

    @Override
    public void webcamGone(WebcamDiscoveryEvent webcamDiscoveryEvent) {
        log.info("webcam gone");
       mainController.removeWebcamMenuItem(webcamDiscoveryEvent.getWebcam().getName());
    }
}
