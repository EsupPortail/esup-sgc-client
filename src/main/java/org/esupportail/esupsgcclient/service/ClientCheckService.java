package org.esupportail.esupsgcclient.service;

import java.net.MalformedURLException;
import java.net.URL;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.printer.ZebraPrinterService;
import org.esupportail.esupsgcclient.task.CheckWebcamTask;
import org.esupportail.esupsgcclient.task.VoidTask;
import org.esupportail.esupsgcclient.ui.MainPane;
import org.esupportail.esupsgcclient.utils.Utils;

import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;


@SuppressWarnings("restriction")
public class ClientCheckService extends Service<Void> {

	private final static Logger log = Logger.getLogger(ClientCheckService.class);

	private String[] args;

	public BooleanProperty clientReady = new SimpleBooleanProperty(false);
	
	private MainPane mainPane;
	
	public ClientCheckService(MainPane mainPane) {
		this.mainPane = mainPane;
	}
	
	@Override
	protected Task<Void> createTask() {

		mainPane.changeStepClientReady("Client en cours de démarrage", "orange");
/*
		try {
			URL urlImage1 = new URL(EncodingService.getSgcUrl() + "/resources/images/logo1.png");
			mainPane.setLogo1(urlImage1);
			URL urlImage2 = new URL(EncodingService.getSgcUrl() + "/resources/images/logo2.png");
			mainPane.setLogo2(urlImage2);

		} catch (MalformedURLException e) {
			log.error("images load error", e);
		}
*/
		try {
			mainPane.changeStepClientReady("Ouverture de la webcam", "orange");

			if (mainPane.webcam != null) {
				CheckWebcamTask checkWebcamTask = new CheckWebcamTask(mainPane.webCamReady);
				checkWebcamTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent t) {
						if (checkWebcamTask.getValue()) {
							mainPane.addLogTextLn("INFO", "webcam : OK");
							try {
								EncodingService.init(args);
								mainPane.addLogTextLn("INFO", "pc/sc : OK");

								if (EncodingService.isEncodeCnous()) {
									mainPane.addLogTextLn("INFO", "dll cnous : OK");
								} else {
									mainPane.hideCnousSteps();
								}

								ZebraPrinterService.init();

								if (ZebraPrinterService.zebraCardPrinter == null && System.getProperty("os.name").contains("windows")) {
									System.load("ZebraNativeUsbAdapter_64");
									ZebraPrinterService.init();
								}

								if (ZebraPrinterService.zebraCardPrinter != null) {
									ZebraPrinterService.cancelJobs();
									mainPane.addLogTextLn("INFO", "zebra : OK");
									mainPane.buttonRestart.setVisible(false);

									mainPane.addLogTextLn("INFO", "numeroId = " + EncodingService.getNumeroId());
									mainPane.addLogTextLn("INFO", "eppnInit = " + EncodingService.getEppnInit());
									mainPane.addLogTextLn("INFO",
											"esupNfcTagServerUrl = " + EncodingService.getEsupNfcTagServerUrl());
									mainPane.addLogTextLn("INFO", "sgcUrl = " + EncodingService.getSgcUrl());

									clientReady.setValue(true);

								} else {
									log.error("zebra: Failed");
									mainPane.addLogTextLn("ERROR", "zebra : Failed");
									mainPane.changeTextPrincipal("Erreur zebra", "red");
								}

							} catch (CnousFournisseurCarteException e) {
								customLog("ERROR", "Erreur de configuration cnous", e);
							} catch (EncodingException | PcscException e) {
								customLog("ERROR", "Erreur lecteur PC/SC", e);
							} catch (ConnectionException e) {
								customLog("ERROR", "Erreur de connexion Zebra", e);
							} catch (ZebraCardException e) {
								customLog("ERROR", "Erreur de communication Zebra", e);
							}
						} else {
							customLog("WARN", "Erreur webcam", null);
							Utils.playSound("R2_fail.wav");
						}
						checkWebcamTask.cancel();
					}
				});

				Thread checkWebcamThread = new Thread(checkWebcamTask);
				checkWebcamThread.setDaemon(true);
				checkWebcamThread.start();

			} else {
				customLog("ERROR", "Erreur de webcam", null);
			}
		} catch (Exception e) {
			customLog("ERROR", "Erreur inconnue", e);
		}
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
				log.warn(message, throwable);
			}
			mainPane.changeTextPrincipal(message, "orange");
			mainPane.addLogTextLn(level, message);
			Utils.playSound("R2_screaming.wav");
		}
	}
	
	public void setArgs(String[] args) {
		this.args = args;
	}

}
