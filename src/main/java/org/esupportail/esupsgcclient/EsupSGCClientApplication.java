package org.esupportail.esupsgcclient;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.EncodingException;
import org.esupportail.esupsgcclient.service.EncodingService;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.webcam.WebcamQRCodeReader;
import org.esupportail.esupsgcclient.ui.EsupSGCJFrame;
import org.esupportail.esupsgcclient.utils.Utils;

public class EsupSGCClientApplication {

	private final static Logger log = Logger.getLogger(EsupSGCClientApplication.class);

	private static WebcamQRCodeReader webcamQRCodeReader;
	private static EncodingService encodingService = new EncodingService();
	private static EsupSGCJFrame esupSGCJFrame = new EsupSGCJFrame();

	public static void main(String... args) {

		addJframeListerners();

		encodingService.init(args);

		if(encodingService.encodeCnous){
			if (encodingService.isCnousOK()) {
				esupSGCJFrame.addLogTextLn("dll cnous : OK");
			} else {
				esupSGCJFrame.addLogTextLn("dll cnous : Failed");
			}
		}else{
			esupSGCJFrame.addLogTextLn("dll cnous : not required");
		}

		esupSGCJFrame.addLogTextLn("numeroId = " + encodingService.numeroId);
		esupSGCJFrame.addLogTextLn("eppnInit = " + encodingService.eppnInit);
		esupSGCJFrame.addLogTextLn("esupNfcTagServerUrl = " + encodingService.esupNfcTagServerUrl);
		esupSGCJFrame.addLogTextLn("sgcUrl = " + encodingService.sgcUrl);
		if (esupSGCJFrame.webcam != null) {
			webcamQRCodeReader = new WebcamQRCodeReader(encodingService.eppnInit, encodingService.sgcUrl,
					esupSGCJFrame);
			while (true) {
				String qrcode = webcamQRCodeReader.readQrCode();
				if (qrcode != null) {
					esupSGCJFrame.initSteps();
					esupSGCJFrame.setStepReadQR(Color.GREEN);
					esupSGCJFrame.addLogTextLn(qrcode + " detected");
					log.info("qrcode detected : " + qrcode);
					try {
						Utils.sleep(1000);
						encodingService.pcscConnection();
						String csn = encodingService.readCsn();

						esupSGCJFrame.setStepReadCSN(Color.GREEN);
						esupSGCJFrame.addLogTextLn("csn : " + csn);

						encodingService.selectForEncoding(qrcode);

						esupSGCJFrame.setStepSelectSGC(Color.GREEN);
						esupSGCJFrame.addLogTextLn(qrcode + " now selected");

						esupSGCJFrame.addLogTextLn("Encoding : Start");

						String encodingResult = encodingService.appsEncoding(csn, esupSGCJFrame);

						if ("END".equals(encodingResult)) {
							esupSGCJFrame.addLogTextLn("Encoding :  OK");
							esupSGCJFrame.setStepEncodageApp(Color.GREEN);

							encodingService.pcscDisconnect();

							if (encodingService.isCnousOK() && encodingService.encodeCnous) {
								esupSGCJFrame.addLogTextLn("Cnous Encoding :  Start");
								encodingService.delCnousCsv();
								if (encodingService.cnousEncoding(csn)) {
									esupSGCJFrame.setStepEncodageCnous(Color.GREEN);
									esupSGCJFrame.addLogTextLn("Cnous Encoding :  OK");
									log.info("cnous encoding : OK");
									esupSGCJFrame.addLogTextLn("Cnous csv start :  OK");
									if (encodingService.sendCnousCsv()) {
										esupSGCJFrame.setStepSendCSV(Color.GREEN);
										esupSGCJFrame.addLogTextLn("Cnous csv send :  OK");
										log.info("cnous csv send : OK");
									} else {
										esupSGCJFrame.setStepSendCSV(Color.RED);
										esupSGCJFrame.addLogTextLn("Cnous csv send :  Failed");
										log.error("cnous csv send : Failed");
									}
								} else {
									esupSGCJFrame.setStepEncodageCnous(Color.RED);
									esupSGCJFrame.addLogTextLn("Cnous Encoding :  Failed");
									log.error("cnous csv send : Failed for qrcode " + qrcode + ", csn " + csn);
								}
							} else {
								esupSGCJFrame.addLogTextLn("Cnous Encoding :  Skipped");
								esupSGCJFrame.setStepEncodageCnous(Color.ORANGE);
								esupSGCJFrame.setStepReadCSN(Color.ORANGE);
							}

						} else {
							esupSGCJFrame.setStepEncodageApp(Color.RED);
							esupSGCJFrame.addLogTextLn("Nothing to do - message from server : " + encodingResult);
							log.warn("Nothing to do - message from server : " + encodingResult);
						}
					} catch (PcscException e) {
						esupSGCJFrame.setStepReadCSN(Color.RED);
						esupSGCJFrame.addLogTextLn("pcsc error : " + e.getMessage());
					} catch (EncodingException e) {
						esupSGCJFrame.setStepEncodageApp(Color.RED);
						esupSGCJFrame.setStepEncodageCnous(Color.RED);
						esupSGCJFrame.setStepSendCSV(Color.RED);
						esupSGCJFrame.addLogTextLn("encoding error : " + e.getMessage());
					} catch (CnousFournisseurCarteException e) {
						esupSGCJFrame.setStepEncodageCnous(Color.RED);
						esupSGCJFrame.addLogTextLn("cnous encoding error : " + e.getMessage());
					}
				}
				while (!encodingService.pcscCardOnTerminal())
					;
				esupSGCJFrame.addLogTextLn("Ready");
			}
		} else {
			log.warn("webcam : Failed");
			esupSGCJFrame.addLogTextLn("webcam : Failed");
		}

	}

	private static void addJframeListerners() {
		esupSGCJFrame.addWindowListener(new WindowAdapter() {
			public void windowClosing(WindowEvent e) {
				exit();
			}
		});
		esupSGCJFrame.buttonExit.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				exit();
			}
		});
	}

	private static void exit() {
		esupSGCJFrame.exit();
		System.gc();
		System.exit(0);
	}

}
