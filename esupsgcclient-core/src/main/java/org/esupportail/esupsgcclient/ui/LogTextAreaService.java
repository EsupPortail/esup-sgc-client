package org.esupportail.esupsgcclient.ui;

import javafx.scene.control.TextArea;
import javafx.scene.text.Text;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.EsupSgcClientJfxController;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

@Component
public class LogTextAreaService {

    final static Logger log = LoggerFactory.getLogger(LogTextAreaService.class);

    TextArea logTextarea;

    Text infoText;

    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");

    String lastMessage = "";

    String lastInfoText = "";

    public void initLogTextArea(TextArea logTextarea, Text infoText) {
        this.logTextarea = logTextarea;
        this.infoText = infoText;
    }

    public void appendText(String text) {
        String currentDate = dateFormat.format(new java.util.Date());
        Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(currentDate + " : " + text + "\n"));
        log.info(text);
    }

    public void setInfoText(String text, String color) {
        Utils.jfxRunLaterIfNeeded(() -> {
            infoText.setText(text);
            infoText.setStyle("-fx-fill: " + color + ";");
        });
        if(!lastInfoText.equals(text)) {
            appendText(text);
            lastInfoText = text;
        }
    }

    public void appendTextNoNewLine(String s) {
         Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(s));
    }

    public void appendTextOnlyOne(String s) {
        if(!lastMessage.equals(s)) {
            lastMessage = s;
            appendText(s);
        }
    }
}
