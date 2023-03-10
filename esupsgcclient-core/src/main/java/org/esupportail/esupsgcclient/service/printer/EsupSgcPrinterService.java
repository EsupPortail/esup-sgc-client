package org.esupportail.esupsgcclient.service.printer;

import javafx.scene.control.MenuBar;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import javafx.stage.Stage;

public abstract class EsupSgcPrinterService {

    public abstract String getMaintenanceInfo() throws Exception;

    public abstract void setupJfxUi(Stage stage, Tooltip tooltip, TextArea logTextarea, MenuBar menuBar);

}
