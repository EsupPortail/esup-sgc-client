package org.esupportail.esupsgcclient.service;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.printer.ZebraPrinterService;
import org.esupportail.esupsgcclient.task.EncodingTask;
import org.esupportail.esupsgcclient.task.VoidTask;
import org.esupportail.esupsgcclient.task.PollPrinterJobStatusTask;
import org.esupportail.esupsgcclient.task.QrcodeReadTask;
import org.esupportail.esupsgcclient.task.ReverseCardTask;
import org.esupportail.esupsgcclient.task.WaitPrinterReadyCardTask;
import org.esupportail.esupsgcclient.ui.MainPane;
import org.esupportail.esupsgcclient.utils.Utils;

import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;

@SuppressWarnings("restriction")
public class MainLoopService extends Service<Void> {

	private final static Logger log = Logger.getLogger(EsupSGCClientApplication.class);

	private int nbCard = 0;

	private MainPane mainPane;
	
	public MainLoopService(MainPane mainPane) {
		this.mainPane = mainPane;
		
		mainPane.buttonRestart.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent ae) {
				log.info("Restart push, try to restart");
				mainPane.addLogTextLn("INFO", "try to restart");
				mainPane.initUi();
				try {
					ZebraPrinterService.cancelJobs();
				} catch (ConnectionException | ZebraCardException e) {
					log.error("enable to cancel jobs : " + e.getMessage());
				}
				setNbCard(0);
				restart();
			}
		});
	}
	
	@Override
	protected Task<Void> createTask() {
		mainPane.addLogTextLn("INFO", getState().toString());
		mainPane.initUi();
		WaitPrinterReadyCardTask waitPrinterReadyCardTask = new WaitPrinterReadyCardTask();
		waitPrinterReadyCardTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				nbCard++;
				mainPane.buttonRestart.setVisible(false);
				mainPane.changeTextPrincipal("Chargement de la carte", "orange");
				mainPane.addLogTextLn("INFO", "get card number : " + nbCard);
				log.info("get card number : " + nbCard);
				PollPrinterJobStatusTask pollPrinterJobStatusTask = new PollPrinterJobStatusTask();
				pollPrinterJobStatusTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent t) {
						if (pollPrinterJobStatusTask.getValue()) {
							mainPane.changeTextPrincipal("Traitement en cours...", "orange");
							mainPane.changeStepReadQR("orange");
							QrcodeReadTask qrcodeReadTask = new QrcodeReadTask(mainPane.imageProperty);
							qrcodeReadTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
								@Override
								public void handle(WorkerStateEvent t) {
									String qrcode = qrcodeReadTask.getValue();
									qrcodeReadTask.cancel();
									if (qrcode != null) {
										mainPane.changeTextPrincipal("Traitement en cours...", "orange");
										mainPane.changeStepReadQR("green");
										mainPane.addLogTextLn("INFO", qrcode + " detected");
										log.info("qrcode detected : " + qrcode);
										mainPane.changeStepReadCSN("orange");
										try {
											EncodingService.pcscConnection();
											String csn = EncodingService.readCsn();
											mainPane.changeStepReadCSN("green");
											mainPane.addLogTextLn("INFO", "csn : " + csn);

											mainPane.changeStepSelectSGC("orange");
											EncodingService.checkBeforeEncoding(qrcode, csn);
											mainPane.changeStepSelectSGC("green");
											mainPane.addLogTextLn("INFO", qrcode + " checked in SGC");

											mainPane.changeStepEncodageApp("orange");
											mainPane.addLogTextLn("INFO", "Encoding : Start");

											EncodingTask encodingTask = new EncodingTask(EncodingService.esupNfcTagServerUrl, EncodingService.numeroId, csn, mainPane.logTextarea);
											encodingTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
												@Override
												public void handle(WorkerStateEvent event) {
													customLog("ERROR", "Erreur d'encodage, voir les logs", encodingTask.getException());
												}
											});	
											encodingTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
												@Override
												public void handle(WorkerStateEvent event) {
													String encodingResult = encodingTask.getValue();
													encodingTask.cancel();
													if ("END".equals(encodingResult)) {
														log.info("encoding ok for : " + qrcode + " - csn : " + csn);
														mainPane.addLogTextLn("INFO", "Encoding :  OK");
														mainPane.changeStepEncodageApp("green");
														try {
															EncodingService.pcscDisconnect();
															if (EncodingService.isCnousOK()
																	&& EncodingService.isEncodeCnous()) {
																mainPane.changeStepEncodageCnous("orange");
																mainPane.addLogTextLn("INFO",
																		"Cnous Encoding :  Start");
																EncodingService.delCnousCsv();
																if (EncodingService.cnousEncoding(csn)) {
																	log.info("cnous encoding : OK");
																	mainPane.changeStepEncodageCnous("green");
																	mainPane.addLogTextLn("INFO", "Cnous Encoding :  OK");
																	mainPane.changeStepSendCSV("orange");
																	mainPane.addLogTextLn("INFO", "Cnous csv start :  OK");
																	if (EncodingService.sendCnousCsv(csn)) {
																		log.info("cnous csv send : OK");
																		mainPane.changeStepSendCSV("green");
																		mainPane.addLogTextLn("INFO", "Cnous csv send :  OK");
																		mainPane.changeTextPrincipal("Encodage terminé", "green");
																	} else {
																		customLog("WARN", "Cnous csv send :  Failed", null);
																		mainPane.changeStepSendCSV("red");
																	}
																} else {
																	customLog("WARN", "cnous csv send : Failed for qrcode " + qrcode + ", csn " + csn, null);
																	mainPane.changeStepEncodageCnous("red;");
																}
															} else {
																mainPane.addLogTextLn("INFO", "Cnous Encoding :  Skipped");
																mainPane.changeTextPrincipal("Encodage terminé", "green");
															}
														} catch (EncodingException e) {
															customLog("ERROR", "Erreur d'encodage, voir les logs", e);
															mainPane.changeStepEncodageApp("red");
														} catch (CnousFournisseurCarteException e) {
															customLog("ERROR", "Erreur CROUS, voir les logs", e);
															mainPane.changeStepEncodageCnous("red");
														} catch (PcscException e) {
															customLog("ERROR", "Erreur lecteur de carte, voir les logs", e);
															mainPane.changeStepReadCSN("red");
														}
													} else {
														customLog("WARN", "Nothing to do - message from server : " + encodingResult, null);
													}

													restart();
												}
											});
											Thread encodingThread = new Thread(encodingTask);
											encodingThread.setDaemon(true);
											encodingThread.start();

										} catch (PcscException e) {
											customLog("ERROR", "Erreur lecteur de carte, voir les logs", e);
											mainPane.changeStepReadCSN("red");

										} catch (SgcCheckException e) {
											customLog("WARN", "Erreur SGC " + e.getMessage(), e);
											mainPane.changeStepSelectSGC("red");
										}
									} else {
										mainPane.changeStepReadQR("red");
										customLog("WARN", "no qrcode found after many retries", null);
									}

								}
							});
							Thread qrcodeReadThread = new Thread(qrcodeReadTask);
							qrcodeReadThread.setDaemon(true);
							while (mainPane.imageProperty.get() == null) {
								Utils.sleep(1000);
							}
							qrcodeReadThread.start();
						} else {
							mainPane.changeStepClientReady("Imprimante non prète", "red");
							mainPane.buttonRestart.setVisible(true);
						}
					}
				});
				Thread pollPrinterJobStatusThread = new Thread(pollPrinterJobStatusTask);
				pollPrinterJobStatusThread.setDaemon(true);
				pollPrinterJobStatusThread.start();
			}
		});
		Thread waitPrinterReadyThread = new Thread(waitPrinterReadyCardTask);
		waitPrinterReadyThread.setDaemon(true);
		waitPrinterReadyThread.start();
		return new VoidTask();
	}

	private void customLog(String level, String message, Throwable throwable) {

		if ("ERROR".equals(level)) {
			if(throwable != null) {
				log.error(message, throwable);
				mainPane.addLogTextLn(level, throwable.getMessage());
				mainPane.addLogTextLn(level, Utils.getExceptionString(throwable));
			}else {
				log.error(message);
				mainPane.addLogTextLn(level, message);
			}
			mainPane.changeTextPrincipal(message, "red");
			mainPane.changeStepClientReady("Client non prêt", "red");
			Utils.playSound("R2_fail.wav");
		} else if ("WARN".equals(level)) {
			if(throwable != null) {
				log.warn(message, throwable);
			} else {
				log.warn(message);
			}
			mainPane.changeTextPrincipal(message, "orange");
			mainPane.addLogTextLn(level, message);
			Utils.playSound("R2_screaming.wav");
		}

		ReverseCardTask reverseCardTask = new ReverseCardTask();
		reverseCardTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				restart();
			}
		});
		Thread reverseCardThread = new Thread(reverseCardTask);
		reverseCardThread.setDaemon(true);
		reverseCardThread.start();
	}

	public void setNbCard(int nbCard) {
		this.nbCard = nbCard;
	}

}
