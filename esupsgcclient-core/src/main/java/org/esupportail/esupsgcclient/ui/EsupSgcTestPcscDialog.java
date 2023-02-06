package org.esupportail.esupsgcclient.ui;


import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.PcscUsbService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

@Component
public class EsupSgcTestPcscDialog {

    private final static Logger log = Logger.getLogger(EsupSgcTestPcscDialog.class);

    public Dialog getTestPcscDialog(Runnable runAtStart, Runnable runAtEnd) {

        log.info("PC/SC Dialog test init");

        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time (sec.)");

        final LineChart<String,Number> lineChart =
                new LineChart<String,Number>(xAxis,yAxis);

        SimpleStringProperty title = new SimpleStringProperty();
        lineChart.titleProperty().bind(title);
        title.setValue("PC/SC Stress Test - 0 erreur(s)");

        XYChart.Series series = new XYChart.Series();
        series.setName("PC/SC connection");

        lineChart.getData().add(series);

        Task testPcSc = new Task<Void>() {

            @Override
            protected Void call() {
                if (runAtStart != null) {
                    runAtStart.run();
                }
                log.info("wait for terminal ...");
                String cardTerminalName = null;
                while (cardTerminalName == null && !isCancelled()) {
                    try {
                        cardTerminalName = PcscUsbService.connection();
                        log.debug("cardTerminal : " + cardTerminalName);
                    } catch (Exception e) {
                        log.trace("PCSC error : " + e.getMessage());
                        Utils.sleep(1000);
                    }
                }
                log.info("terminal ok : " + cardTerminalName);
                log.info("test run for 20 sec");
                boolean cardWasOk = false;
                int nbFailed = 0;
                long time = System.currentTimeMillis();
                String lastTimeFailed = "";
                while (System.currentTimeMillis() - time < 20200 && !isCancelled()) {
                    boolean isCardOk = false;
                    try {
                        // Test : card is prsent AND get UID ok AND get challenge (for auth) ok
                        isCardOk = PcscUsbService.isCardPresent() && !StringUtils.isEmpty(PcscUsbService.getCardId()) && !StringUtils.isEmpty(PcscUsbService.sendAPDU("901a0000010000"));
                        cardWasOk = cardWasOk || isCardOk;
                    } catch (Exception e) {
                        log.error(String.format("Exception after %.2f sec. - reconnect terminal", (System.currentTimeMillis() - time) / 1000.0), e);
                        try {
                            cardTerminalName = PcscUsbService.connection();
                        } catch (Exception ex) {
                            log.warn(String.format("Can't reconnect terminal at %.2f sec", (System.currentTimeMillis() - time) / 1000.0), e);
                        }
                        log.debug("cardTerminal : " + cardTerminalName);
                    }
                    final String fk = String.format("%.1f sec", (System.currentTimeMillis() - time) / 1000.0);
                    final int fv = isCardOk ? 1 : 0;
                    lastTimeFailed = isCardOk ? lastTimeFailed : fk;
                    Platform.runLater(() ->
                            {
                                series.getData().add(new XYChart.Data(fk, fv));
                            }
                    );
                    if (cardWasOk && !isCardOk) {
                        log.warn("card no more present after " + fk);
                        nbFailed++;
                        final String newTitle = String.format("PC/SC Stress Test - %d error(s) - last error : %s", nbFailed, lastTimeFailed);
                        Platform.runLater(() ->
                        {
                            title.setValue(newTitle);
                        });
                    }
                    Utils.sleep(100);
                }
                log.info("test finished - nb failed : " + nbFailed);
                if (runAtEnd != null) {
                    runAtEnd.run();
                }
                return null;
            }
        };


        DialogPane dialogPane = new DialogPane();
        dialogPane.setContent(lineChart);

        Dialog pcscTestDialog = new Dialog<>();
        pcscTestDialog.setDialogPane(dialogPane);

        pcscTestDialog.setOnShown(dialogEvent -> new Thread(testPcSc).start());

        Window window = pcscTestDialog.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> {
            testPcSc.cancel();
            window.hide();
        });

        log.info("PC/SC Dialog test init OK");

        return pcscTestDialog;
    }
}
