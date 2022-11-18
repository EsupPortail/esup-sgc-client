package org.esupportail.esupsgcclient.service;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.task.CheckWebcamTask;
import org.esupportail.esupsgcclient.ui.MainController;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;

public class ClientCheckService extends Task<Void> {

	private final static Logger log = Logger.getLogger(ClientCheckService.class);

	//private String[] args;

	public BooleanProperty clientReady = new SimpleBooleanProperty(false);

	private MainController mainPane;

	public ClientCheckService(MainController mainPane) {
		this.mainPane = mainPane;
	}

	@Override
	protected Void call() {

		try {
			if (mainPane.webcam != null) {
				CheckWebcamTask checkWebcamTask = new CheckWebcamTask(mainPane.webCamReady);
				checkWebcamTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent t) {
						if (checkWebcamTask.getValue()) {
							mainPane.changeStepClientReady("Ouverture de la webcam", "orange");
							mainPane.addLogTextLn("INFO", "webcam : OK");
							try {
								EncodingService.init();
								mainPane.addLogTextLn("INFO", "pc/sc : OK");

								if (EncodingService.isEncodeCnous()) {
									mainPane.addLogTextLn("INFO", "dll cnous : OK");
								} else {
									mainPane.hideCnousSteps();
								}

								mainPane.buttonRestart.setVisible(false);
								mainPane.addLogTextLn("INFO", "numeroId = " + EncodingService.getNumeroId());
								mainPane.addLogTextLn("INFO", "sgcAuthToken = " + EncodingService.getSgcAuthToken());
								mainPane.addLogTextLn("INFO", "esupNfcTagServerUrl = " + EncodingService.getEsupNfcTagServerUrl());
								mainPane.addLogTextLn("INFO", "sgcUrl = " + EncodingService.getSgcUrl());
								clientReady.setValue(true);
								
							} catch (CnousFournisseurCarteException e) {
								customLog("ERROR", "Erreur de configuration cnous", e);
							} catch (EncodingException | PcscException e) {
								customLog("ERROR", "Erreur lecteur PC/SC", e);
							}
						} else {
							customLog("WARN", "Erreur webcam", null);
						}
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
		return null;
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
		} else if ("WARN".equals(level)) {
			if(throwable != null) {
				log.warn(message, throwable);
			} else {
				log.warn(message, throwable);
			}
			mainPane.changeTextPrincipal(message, "orange");
			mainPane.addLogTextLn(level, message);
		}
		Utils.playSound("fail.wav");
	}

}
