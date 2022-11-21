package org.esupportail.esupsgcclient.service;

import javafx.concurrent.Service;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.EncodingException;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.task.EncodingTask;
import org.esupportail.esupsgcclient.task.WaitRemoveCardTask;
import org.esupportail.esupsgcclient.ui.MainController;
import org.esupportail.esupsgcclient.utils.Utils;

public abstract class SgcLoopService<V> extends Service<V> {

    private final static Logger log = Logger.getLogger(QrCodeEncodeLoopService.class);

    void encode(String qrcode, boolean encodeWithPrinter) {
        log.info("begin encode card with qrcode : " + qrcode);
        try {
            EncodingService.pcscConnection();
            String csn = EncodingService.readCsn();
            EncodingService.checkBeforeEncoding(qrcode, csn);
            EncodingTask encodingTask = new EncodingTask(EncodingService.esupNfcTagServerUrl, EsupSGCClientApplication.numeroId, csn);
            encodingTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    if(!encodeWithPrinter) {
                        WaitRemoveCardTask waitRemoveCardTask = new WaitRemoveCardTask();
                        waitRemoveCardTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                            @Override
                            public void handle(WorkerStateEvent t) {
                                restart();
                            }
                        });
                        Thread waitRemoveCardThread = new Thread(waitRemoveCardTask);
                        waitRemoveCardThread.setDaemon(true);
                        waitRemoveCardThread.start();
                    } else {
                        EvolisPrinterService.reject();
                        restart();
                    }
                }
            });
            encodingTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                @Override
                public void handle(WorkerStateEvent event) {
                    String encodingResult = encodingTask.getValue();
                    encodingTask.cancel();
                    if ("END".equals(encodingResult)) {
                        log.info("encoding ok for : " + qrcode + " - csn : " + csn);
                        try {
                            EncodingService.pcscDisconnect();
                            if (EncodingService.isCnousOK() && EncodingService.isEncodeCnous()) {
                                log.info("Cnous Encoding :  Start");
                                EncodingService.delCnousCsv();
                                if (EncodingService.cnousEncoding(csn)) {
                                    log.info("cnous encoding : OK");
                                    if (EncodingService.sendCnousCsv(csn)) {
                                        log.info("cnous csv send : OK");
                                        Utils.playSound("success.wav");
                                    } else {
                                        log.warn("Cnous csv send :  Failed");
                                    }
                                } else {
                                    log.warn("cnous csv send : Failed for qrcode " + qrcode + ", csn " + csn, null);
                                }
                            } else {
                                log.info("Cnous Encoding :  Skipped");
                                log.info("Encodage terminé");
                                Utils.playSound("success.wav");
                            }
                        } catch (EncodingException e) {
                            log.error("Erreur d'encodage, voir les logs", e);
                        } catch (CnousFournisseurCarteException e) {
                            log.error("Erreur CROUS, voir les logs", e);
                        } catch (PcscException e) {
                            log.error("Erreur lecteur de carte, voir les logs", e);
                        }
                    } else {
                        log.warn("Nothing to do - message from server : " + encodingResult);
                    }

                    if(!encodeWithPrinter) {
                        WaitRemoveCardTask waitRemoveCardTask = new WaitRemoveCardTask();
                        waitRemoveCardTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                            @Override
                            public void handle(WorkerStateEvent t) {
                                restart();
                            }
                        });
                        log.info("please change card");
                        Thread waitRemoveCardThread = new Thread(waitRemoveCardTask);
                        waitRemoveCardThread.setDaemon(true);
                        waitRemoveCardThread.start();
                    } else {
                        EvolisPrinterService.eject();
                        restart();
                    }
                }
            });
            Thread encodingThread = new Thread(encodingTask);
            encodingThread.setDaemon(true);
            encodingThread.start();

        } catch (PcscException e) {
            log.error("Erreur lecteur de carte, voir les logs", e);
            Utils.sleep(1000);
            restart();
        } catch (SgcCheckException e) {
            log.warn("Erreur SGC " + e.getMessage(), e);
            if(!encodeWithPrinter) {
                WaitRemoveCardTask waitRemoveCardTask = new WaitRemoveCardTask();
                waitRemoveCardTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
                    @Override
                    public void handle(WorkerStateEvent t) {
                        restart();
                    }
                });
                log.info("please change card");
                Thread waitRemoveCardThread = new Thread(waitRemoveCardTask);
                waitRemoveCardThread.setDaemon(true);
                waitRemoveCardThread.start();
            } else {
                EvolisPrinterService.reject();
                restart();
            }
        }
    }

}
