package org.esupportail.esupsgcclient.task;

import com.github.sarxos.webcam.Webcam;
import javafx.beans.property.ObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.InitEncodingServiceTask;
import org.esupportail.esupsgcclient.utils.Utils;

import java.awt.image.BufferedImage;
import java.util.concurrent.atomic.AtomicReference;

public class WebcamUiTask extends Task<Void> {

    final static Logger log = Logger.getLogger(WebcamUiTask.class);

    Webcam webcam;
    ObjectProperty<Image> imageProperty;

    public WebcamUiTask(Webcam webcam, ObjectProperty<Image> imageProperty) {
        this.webcam = webcam;
        this.imageProperty = imageProperty;
    }

    @Override
    protected Void call() throws Exception {
        final AtomicReference<WritableImage> ref = new AtomicReference<>();
        BufferedImage webcamBufferedImage;
        while (true) {
            try {
                if (webcam != null && (webcamBufferedImage = webcam.getImage()) != null) {
                    ref.set(SwingFXUtils.toFXImage(webcamBufferedImage, ref.get()));
                    webcamBufferedImage.flush();
                    imageProperty.set(ref.get());
                }
            } catch (Exception e) {
                log.warn("pb", e);
            }
            Utils.sleep(100);
        }
    }
}
