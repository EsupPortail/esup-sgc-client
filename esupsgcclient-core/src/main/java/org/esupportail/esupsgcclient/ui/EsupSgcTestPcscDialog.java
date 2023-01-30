package org.esupportail.esupsgcclient.ui;


import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.stage.Window;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.PcscUsbService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import javax.smartcardio.CardException;

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

        Thread testPcSc = new Thread(() -> {
            if(runAtStart != null) {
                runAtStart.run();
            }
            log.info("wait for terminal ...");
            String cardTerminalName = null;
            while(cardTerminalName==null) {
                try {
                    cardTerminalName = PcscUsbService.connection();
                    log.debug("cardTerminal : " + cardTerminalName);
                } catch (CardException e) {
                    log.trace("pcsc connection error : " + e.getMessage());
                    Utils.sleep(1000);
                }
            }
            log.info("terminal ok : " + cardTerminalName);
            log.info("test run for 10 sec");
            boolean cardWasPresent = false;
            int nbFailed = 0;
            long time = System.currentTimeMillis();
            while(System.currentTimeMillis()-time<10200) {
                boolean isCardPresent = false;
                try {
                    isCardPresent = PcscUsbService.isCardPresent();
                    cardWasPresent = cardWasPresent || isCardPresent;
                } catch (CardException e) {
                    e.printStackTrace();
                }
                final String fk = String.format("%.1f", (System.currentTimeMillis()-time)/1000.0);
                final int fv = isCardPresent ? 1 : 0;
                Platform.runLater(() ->
                        {
                            series.getData().add(new XYChart.Data(fk, fv));
                        }
                );
                if(cardWasPresent && !isCardPresent) {
                    log.warn("card no more present after " + fk + " sec");
                    nbFailed++;
                    final String newTtitle = String.format("PC/SC Stress Test - %d erreur(s)", nbFailed);
                    Platform.runLater(() ->
                    {
                        title.setValue(newTtitle);
                    });
                }
                Utils.sleep(100);
            }
            log.info("test finished - nb failed : " + nbFailed);
            if(runAtEnd != null) {
                runAtEnd.run();
            }
        });

        DialogPane dialogPane = new DialogPane();
        dialogPane.setContent(lineChart);

        Dialog pcscTestDialog = new Dialog<>();
        pcscTestDialog.setDialogPane(dialogPane);

        pcscTestDialog.setOnShown(dialogEvent -> testPcSc.start());

        Window window = pcscTestDialog.getDialogPane().getScene().getWindow();
        window.setOnCloseRequest(event -> window.hide());

        log.info("PC/SC Dialog test init OK");

        return pcscTestDialog;
    }
}
