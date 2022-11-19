package org.esupportail.esupsgcclient.service.webcam;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamDiscoveryEvent;
import com.github.sarxos.webcam.WebcamDiscoveryListener;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.ui.MainController;


public class EsupWebcamDiscoveryListener implements WebcamDiscoveryListener {

    final static Logger log = Logger.getLogger(EsupWebcamDiscoveryListener.class);

    MainController mainController;

    public EsupWebcamDiscoveryListener(MainController mainController) {
        this.mainController = mainController;
    }

    @Override
    public void webcamFound(WebcamDiscoveryEvent webcamDiscoveryEvent) {
        log.info("webcam found");
        if(mainController.webcam != webcamDiscoveryEvent.getWebcam()) {
            webcamDiscoveryEvent.getWebcam().open();
            mainController.webcam = webcamDiscoveryEvent.getWebcam();
            mainController.webcamReady.set(true);
        }
    }

    @Override
    public void webcamGone(WebcamDiscoveryEvent webcamDiscoveryEvent) {
        log.info("webcam gone");
        webcamDiscoveryEvent.getWebcam().close();
        if(mainController.webcam == webcamDiscoveryEvent.getWebcam()) {
            mainController.webcam = null;
        }
        mainController.webcamReady.set(false);
    }
}
