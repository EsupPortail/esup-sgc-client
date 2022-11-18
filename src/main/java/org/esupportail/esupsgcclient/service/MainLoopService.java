package org.esupportail.esupsgcclient.service;

import javafx.scene.image.Image;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.task.EncodingTask;
import org.esupportail.esupsgcclient.task.EsupSgcLongPollTask;
import org.esupportail.esupsgcclient.task.EvolisTask;
import org.esupportail.esupsgcclient.task.QrcodeReadTask;
import org.esupportail.esupsgcclient.task.SleepTask;
import org.esupportail.esupsgcclient.task.VoidTask;
import org.esupportail.esupsgcclient.task.WaitRemoveCardTask;
import org.esupportail.esupsgcclient.ui.MainPane;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.EventHandler;
import org.springframework.web.client.RestClientException;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

public class MainLoopService extends Service<Void> {

	private final static Logger log = Logger.getLogger(EsupSGCClientApplication.class);

	private static long restartDelay = 4000;

	private MainPane mainPane;

	public MainLoopService(MainPane mainPane) {
		this.mainPane = mainPane;
	}

	@Override
	protected Task<Void> createTask() {
		mainPane.addLogTextLn("INFO", getState().toString());
		mainPane.initUi();
		mainPane.setOk();

		mainPane.changeStepReadQR("orange");

		// mainPane.webCamPane.setLeft(null);
		// mainPane.webCamPane.setCenter(mainPane.webcamImageView);

		QrcodeReadTask qrcodeReadTask = new QrcodeReadTask(mainPane.imageProperty);
		EsupSgcLongPollTask esupSgcLongPollTask = new EsupSgcLongPollTask();

		qrcodeReadTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				String qrcode = qrcodeReadTask.getValue();
				qrcodeReadTask.cancel();
				esupSgcLongPollTask.cancel();
				encode(qrcode, false);
			}
		});

		esupSgcLongPollTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				String qrcode = esupSgcLongPollTask.getValue();
				qrcodeReadTask.cancel();
				esupSgcLongPollTask.cancel();
				mainPane.addLogTextLn("INFO", "Card with qrcode " + qrcode + " should be edited with printer");
				printAndEncode(qrcode);
			}
		});

		Thread qrcodeReadThread = new Thread(qrcodeReadTask);
		qrcodeReadThread.setDaemon(true);
		qrcodeReadThread.start();

		Thread esupSgcLongPollThread = new Thread(esupSgcLongPollTask);
		esupSgcLongPollThread.setDaemon(true);
		esupSgcLongPollThread.start();

		while (mainPane.imageProperty.get() == null) {
			Utils.sleep(1000);
		}
		return new VoidTask();
	}

	private void printAndEncode(String qrcode) {
		String bmpColorAsBase64 = null;
		String bmpBlackAsBase64 = null;
		try {
			bmpColorAsBase64 = EncodingService.getBmpColorAsBase64(qrcode);
			bmpBlackAsBase64 = EncodingService.gatBmpBlackAsBase64(qrcode);
			try {
				byte[] bmpColor = Base64.getDecoder().decode(bmpColorAsBase64.getBytes());
				BufferedImage input_image = ImageIO.read(new ByteArrayInputStream(bmpColor)); //
				ByteArrayOutputStream out = new ByteArrayOutputStream();// read bmp into input_image object
				ImageIO.write(input_image, "PNG", out);
				mainPane.bmpColorImageView.setImage(new Image(new ByteArrayInputStream( out.toByteArray()), 200, 200, true, true));
				byte[] bmpBlack = Base64.getDecoder().decode(bmpBlackAsBase64.getBytes());
				input_image = ImageIO.read(new ByteArrayInputStream(bmpBlack)); //
				out = new ByteArrayOutputStream();// read bmp into input_image object
				ImageIO.write(input_image, "PNG", out);
				mainPane.bmpBlackImageView.setImage(new Image(new ByteArrayInputStream( out.toByteArray()), 200, 200, true, true));
				mainPane.webCamPane.setLeft(mainPane.bmpBlackImageView);
				mainPane.webCamPane.setCenter(mainPane.bmpColorImageView);
			} catch (IOException e) {
				log.warn("Can't display bmp", e);
			}
		} catch(RestClientException ex) {
			throw new RuntimeException("Can't get BMP from esup-sgc for this qrcode " + qrcode, ex);
		}
		EvolisTask evolisTask = new EvolisTask(bmpColorAsBase64, bmpBlackAsBase64);
		evolisTask.setOnFailed(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				customLog("ERROR", "Erreur d'encodage, voir les logs", evolisTask.getException());
				EvolisPrinterService.reject();
				restart();
			}
		});
		evolisTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent event) {
				encode(qrcode, true);
				// Utils.sleep(5000);
				// EvolisPrinterService.eject();
				// restart();
			}
		});
		Thread evolisThread = new Thread(evolisTask);
		evolisThread.setDaemon(true);
		evolisThread.start();
	}

	void encode(String qrcode, boolean encodeWithPrinter) {
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
					mainPane.changeStepEncodageApp("red");
					if(!encodeWithPrinter) {
						WaitRemoveCardTask waitRemoveCardTask = new WaitRemoveCardTask();
						waitRemoveCardTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
							@Override
							public void handle(WorkerStateEvent t) {
								restart();
							}
						});
						mainPane.addLogTextLn("INFO", "please change card");
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
						mainPane.addLogTextLn("INFO", "Encoding :  OK");
						mainPane.changeStepEncodageApp("green");
						try {
							EncodingService.pcscDisconnect();
							if (EncodingService.isCnousOK() && EncodingService.isEncodeCnous()) {
								mainPane.changeStepEncodageCnous("orange");
								mainPane.addLogTextLn("INFO", "Cnous Encoding :  Start");
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
										Utils.playSound("success.wav");
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
								Utils.playSound("success.wav");
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

					if(!encodeWithPrinter) {
						WaitRemoveCardTask waitRemoveCardTask = new WaitRemoveCardTask();
						waitRemoveCardTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
							@Override
							public void handle(WorkerStateEvent t) {
								restart();
							}
						});
						mainPane.addLogTextLn("INFO", "please change card");
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
			customLog("ERROR", "Erreur lecteur de carte, voir les logs", e);
			mainPane.changeStepReadCSN("red");
			SleepTask sleepTask = new SleepTask(restartDelay);
			sleepTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
				@Override
				public void handle(WorkerStateEvent t) {
					restart();
				}
			});

			Thread sleepThread = new Thread(sleepTask);
			sleepThread.setDaemon(true);
			sleepThread.start();
		} catch (SgcCheckException e) {
			customLog("WARN", "Erreur SGC " + e.getMessage(), e);
			mainPane.changeStepSelectSGC("red");
			if(!encodeWithPrinter) {
				WaitRemoveCardTask waitRemoveCardTask = new WaitRemoveCardTask();
				waitRemoveCardTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent t) {
						restart();
					}
				});
				mainPane.addLogTextLn("INFO", "please change card");
				Thread waitRemoveCardThread = new Thread(waitRemoveCardTask);
				waitRemoveCardThread.setDaemon(true);
				waitRemoveCardThread.start();
			} else {
				EvolisPrinterService.reject();
				restart();
			}
		}
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
				log.warn(message);
			}
			mainPane.changeTextPrincipal(message, "orange");
			mainPane.addLogTextLn(level, message);
		}
		Utils.playSound("fail.wav");
	}

}
