package org.esupportail.esupsgcclient.service.printer.evolis;


import jakarta.annotation.Resource;
import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.Scene;
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
import java.util.Random;

@Component
public class EvolisTestPcsc {

    private final static Logger log = Logger.getLogger(EvolisTestPcsc.class);

    @Resource
    EvolisPrinterCommands evolisPrinterCommands;

    public Dialog getTestPcscDialog() {

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
            log.info("insertCardToContactLessStation");
            evolisPrinterCommands.insertCardToContactLessStation();
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
            long time = System.currentTimeMillis();
            int k = 0;
            log.info("test run for 10 sec");
            boolean cardWasPresent = false;
            int nbFailed = 0;
            while(System.currentTimeMillis()-time<10000) {
                final String fk = Double.toString(k/10.0);
                boolean isCardPresent = false;
                try {
                    isCardPresent = PcscUsbService.isCardPresent();
                    cardWasPresent = cardWasPresent || isCardPresent;
                } catch (CardException e) {
                    e.printStackTrace();
                }
                final int fv = isCardPresent ? 1 : 0;
                Platform.runLater(() ->
                        {
                            series.getData().add(new XYChart.Data(fk, fv));
                        }
                );
                if(cardWasPresent && !isCardPresent) {
                    log.warn("card no more present after " + k/10.0 + " sec");
                    nbFailed++;
                    final String newTtitle = String.format("PC/SC Stress Test - %d erreur(s)", nbFailed);
                    Platform.runLater(() ->
                    {
                        title.setValue(newTtitle);
                    });
                }
                Utils.sleep(100);
                k++;
            }
            log.info("test finished - nb failed : " + nbFailed);

            evolisPrinterCommands.eject();
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
