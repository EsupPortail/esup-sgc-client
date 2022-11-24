package org.esupportail.esupsgcclient.service.webcam;

import com.github.sarxos.webcam.Webcam;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSgcClientJfxController;
import org.esupportail.esupsgcclient.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

public class WebcamTaskService extends Service<Void> {

    final static Logger log = Logger.getLogger(WebcamTaskService.class);

    final String webcamName;

    final ImageView webcamImageView;

    public WebcamTaskService(String webcamName, ImageView webcamImageView) {
        this.webcamName = webcamName;
        this.webcamImageView = webcamImageView;
    }

    @Override
    protected Task<Void> createTask() {
        Task<Void> webcamUiTask = new Task<Void>() {
            @Override
            protected Void call() throws Exception {
                final AtomicReference<WritableImage> ref = new AtomicReference<>();
                Webcam webcam = Webcam.getWebcamByName(webcamName);
                webcam.open();
                ObjectProperty<Image> imageProperty = webcamImageView.imageProperty();
                webcamImageView.setRotate(180);
                BufferedImage webcamBufferedImage = null;
                BufferedImage newWebcamBufferedImage = null;
                EsupSgcClientJfxController.webcamReady.set(true);
                while (true) {
                    try {
                        if(this.isCancelled()) {
                            imageProperty.unbind();
                            webcam.close();
                            EsupSgcClientJfxController.webcamReady.set(false);
                            log.info("-> cancel");
                            return null;
                        }
                        if ((newWebcamBufferedImage = webcam.getImage()) != null) {
                            webcamBufferedImage = newWebcamBufferedImage;
                            ref.set(SwingFXUtils.toFXImage(webcamBufferedImage, ref.get()));
                            webcamBufferedImage.flush();
                            imageProperty.set(ref.get());
                        } else {
                            log.warn("image is null");
                            this.cancel();
                        }
                    } catch (Exception e) {
                        log.warn("pb with camera", e);
                        this.cancel();
                    }
                    Utils.sleep(50);
                }
            }
        };
        return webcamUiTask;
    }

}
