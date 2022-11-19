package org.esupportail.esupsgcclient.service;

import javafx.application.Platform;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.ui.MainController;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.concurrent.Task;

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
								EncodingService.init();
								mainPane.nfcReady.setValue(true);
								mainPane.addLogTextLn("INFO", "pc/sc : OK");

								if (EncodingService.isEncodeCnous()) {
									mainPane.addLogTextLn("INFO", "dll cnous : OK");
								} else {
									mainPane.hideCnousSteps();
								}

								mainPane.addLogTextLn("INFO", "numeroId = " + EncodingService.getNumeroId());
								mainPane.addLogTextLn("INFO", "sgcAuthToken = " + EncodingService.getSgcAuthToken());
								mainPane.addLogTextLn("INFO", "esupNfcTagServerUrl = " + EncodingService.getEsupNfcTagServerUrl());
								mainPane.addLogTextLn("INFO", "sgcUrl = " + EncodingService.getSgcUrl());
								clientReady.setValue(true);
								
							} catch (CnousFournisseurCarteException e) {
								customLog("ERROR", "Erreur de configuration cnous", e);
							} catch (EncodingException | PcscException e) {
								mainPane.nfcReady.setValue(false);
								customLog("ERROR", "Erreur lecteur PC/SC", e);
							}
							return null;
					}


	private void customLog(String level, String message, Throwable throwable) {
		Platform.runLater(new Runnable() {
			@Override
			public void run() {
				if ("ERROR".equals(level)) {
					if (throwable != null) {
						log.error(message, throwable);
						mainPane.addLogTextLn(level, throwable.getMessage());
						mainPane.addLogTextLn(level, Utils.getExceptionString(throwable));
					} else {
						log.error(message);
						mainPane.addLogTextLn(level, message);
					}
					mainPane.changeTextPrincipal(message, MainController.StyleLevel.danger);
					mainPane.changeStepClientReady("Client non prêt", MainController.StyleLevel.danger);
				} else if ("WARN".equals(level)) {
					if (throwable != null) {
						log.warn(message, throwable);
					} else {
						log.warn(message, throwable);
					}
					mainPane.changeTextPrincipal(message, MainController.StyleLevel.warning);
					mainPane.addLogTextLn(level, message);
				}
				Utils.playSound("fail.wav");
			}
		});
	}

}
