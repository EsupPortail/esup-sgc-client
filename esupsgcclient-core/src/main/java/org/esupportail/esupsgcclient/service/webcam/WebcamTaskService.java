package org.esupportail.esupsgcclient.service.webcam;

import com.github.sarxos.webcam.Webcam;
import javax.annotation.Resource;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

@Component
public class WebcamTaskService extends Service<Void> {

    final static Logger log = Logger.getLogger(WebcamTaskService.class);

    String webcamName;

    ImageView webcamImageView;

    @Resource
    AppSession appSession;

    public void init(String webcamName, ImageView webcamImageView) {
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
                webcam.setViewSize(new Dimension(640, 480));
                webcam.open();
                ObjectProperty<Image> imageProperty = webcamImageView.imageProperty();
                webcamImageView.setRotate(180);

                appSession.setWebcamReady(true);
                while (true) {
                    try {
                        if(this.isCancelled()) {
                            imageProperty.unbind();
                            webcam.close();
                            log.info("webcam canceled");
                            appSession.setWebcamReady(false);
                            return null;
                        }
                        BufferedImage webcamBufferedImage = webcam.getImage();
                        if (webcamBufferedImage != null) {
                            Utils.jfxRunLaterIfNeeded(() -> {
                                ref.set(SwingFXUtils.toFXImage(webcamBufferedImage, ref.get()));
                                imageProperty.set(ref.get());
                                appSession.setWebcamReady(true);
                                webcamBufferedImage.flush();
                            });
                        } else {
                            log.warn("image is null");
                            this.cancel();
                        }
                    } catch (Exception e) {
                        log.warn("pb with camera", e);
                        this.cancel();
                    }
                    Utils.sleep(200);
                }
            }
        };
        return webcamUiTask;
    }

}
