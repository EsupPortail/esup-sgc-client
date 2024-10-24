package org.esupportail.esupsgcclient.ui;

import javafx.scene.control.TextArea;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSgcClientJfxController;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import java.text.DateFormat;
import java.text.SimpleDateFormat;

@Component
public class LogTextAreaService {

    final static Logger log = Logger.getLogger(LogTextAreaService.class);

    TextArea logTextarea;

    DateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss:SSS");

    public void initLogTextArea(TextArea logTextarea) {
        this.logTextarea = logTextarea;
    }

    public void appendText(String text) {
        String currentDate = dateFormat.format(new java.util.Date());
        Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(currentDate + " : " + text + "\n"));
        log.info(text);
    }

}
