package org.esupportail.esupsgcclient.taskencoding;

import javafx.beans.property.ObjectProperty;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;

public class TaskParamBean {

    public enum RootType {qrcode, evolis}

    final RootType rootType;
    final String qrcode;

    final ObjectProperty<Image> webcamImageProperty;

    final String csn;

    final EncodingService.BmpType bmpType;

    final ImageView bmpColorImageView;

    final ImageView bmpBlackImageView;

    final String bmpColorAsBase64;

    final String bmpBlackAsBase64;

    final Boolean eject4success;

    final Boolean fromPrinter;

    public TaskParamBean(RootType rootType, String qrcode, ObjectProperty<Image> imageProperty, String csn,
                         EncodingService.BmpType bmpType, ImageView bmpColorImageView, ImageView bmpBlackImageView,
                         String bmpColorAsBase64, String bmpBlackAsBase64,
                         Boolean eject4success, Boolean fromPrinter) {
        this.rootType = rootType;
        this.qrcode = qrcode;
        this.webcamImageProperty = imageProperty;
        this.csn = csn;
        this.bmpType = bmpType;
        this.bmpColorImageView = bmpColorImageView;
        this.bmpBlackImageView = bmpBlackImageView;
        this.bmpColorAsBase64 = bmpColorAsBase64;
        this.bmpBlackAsBase64 = bmpBlackAsBase64;
        this.eject4success = eject4success;
        this.fromPrinter = fromPrinter;
    }

}
