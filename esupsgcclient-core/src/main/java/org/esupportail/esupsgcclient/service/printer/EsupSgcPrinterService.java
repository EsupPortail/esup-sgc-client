package org.esupportail.esupsgcclient.service.printer;

import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;

public abstract class EsupSgcPrinterService {

    public abstract String getMaintenanceInfo();

    public abstract void setupCheckPrinterToolTip(Tooltip tooltip, TextArea logTextarea);

    public abstract void reject();

    public abstract void printEnd();
}
