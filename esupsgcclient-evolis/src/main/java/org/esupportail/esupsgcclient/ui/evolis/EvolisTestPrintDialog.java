package org.esupportail.esupsgcclient.ui.evolis;


import javafx.beans.property.SimpleStringProperty;
import javafx.concurrent.Task;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.LineChart;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Dialog;
import javafx.scene.control.DialogPane;
import javafx.scene.control.TextArea;
import javafx.stage.Window;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.PcscUsbService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisSocketException;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import javax.smartcardio.CardException;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;

@Component
public class EvolisTestPrintDialog {

    private final static Logger log = Logger.getLogger(EvolisTestPrintDialog.class);

    public Dialog getEvolisTestPrintDialog(EvolisPrinterService evolisPrinterService, TextArea logTextarea) {

        log.info("PC/SC Dialog test init");

        final CategoryAxis xAxis = new CategoryAxis();
        final NumberAxis yAxis = new NumberAxis();
        xAxis.setLabel("Time (sec.)");

        final LineChart<String,Number> lineChart =
                new LineChart<String,Number>(xAxis,yAxis);

        SimpleStringProperty title = new SimpleStringProperty();
        lineChart.titleProperty().bind(title);
        title.setValue("Evolis Print Stress Test - 0");

        XYChart.Series series = new XYChart.Series();
        series.setName("Evolis Print Stress");

        lineChart.getData().add(series);

        Task testPcSc = new Task<Void>() {

            @Override
            protected Void call() {

                long time = System.currentTimeMillis();
                int nbTest = 0;
                Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis started for 60 sec. ...\n"));
                while (System.currentTimeMillis() - time < 60000 && !isCancelled()) {
                    nbTest++;
                    String bmpColorAsBase64 = getBmpAsBase64("test/color.bmp");
                    String bmpBlackAsBase64 = getBmpAsBase64("test/black.bmp");
                    String bmpOverlayAsBase64 = getBmpAsBase64("test/overlay.bmp");
                    Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis start sequence ...\n"));
                    evolisPrinterService.startSequence();
                    Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis no eject ...\n"));
                    evolisPrinterService.noEject();
                    Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis print begin ...\n"));
                    evolisPrinterService.printBegin();
                    Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis print set ...\n"));
                    evolisPrinterService.printSet();
                    Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis send color ...\n"));
                    evolisPrinterService.printFrontColorBmp(bmpColorAsBase64);
                    Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis send black ...\n"));
                    evolisPrinterService.printFrontBlackBmp(bmpBlackAsBase64);
                    Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis send overlay ...\n"));
                    evolisPrinterService.printFrontVarnish(bmpOverlayAsBase64);
                    try {
                        Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis end ...\n"));
                        evolisPrinterService.printEnd();
                    } catch (EvolisSocketException e) {
                        throw new RuntimeException(e);
                    }
                    Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Stress print test evolis end OK\n"));
                    final String fk = String.format("%.1f sec", (System.currentTimeMillis() - time) / 1000.0);
                    final int fv = 1;
                    final String newTitle = String.format("Evolis Print Stress Test - %d", nbTest);
                    Utils.jfxRunLaterIfNeeded(() ->
                            {
                                series.getData().add(new XYChart.Data(fk, fv));
                                title.setValue(newTitle);
                            }
                    );
                }
                final String nbTests = Integer.toString(nbTest);
                final String seconds = Long.toString((System.currentTimeMillis() - time)/1000);
                Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(nbTests + " stress print tests in " + seconds + "sec.\n"));
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

        log.info("Evolis Print Dialog Test init OK");

        return pcscTestDialog;
    }

    protected String getBmpAsBase64(String bmpResourceFile) {
        try {
            InputStream bmpIn = new ClassPathResource(bmpResourceFile).getInputStream();
            byte[] encodedBmp = Base64.encodeBase64(IOUtils.toByteArray(bmpIn));
            String bmpCard = new String(encodedBmp, StandardCharsets.US_ASCII);
            return bmpCard;
        } catch (IOException e) {
            throw new RuntimeException("Exception getting BMP " + bmpResourceFile + " as base64  : check installation", e);
        }
    }

}
