package org.esupportail.esupsgcclient;

import java.awt.Color;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.io.InputStream;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.URL;

import javax.imageio.ImageIO;
import javax.swing.ImageIcon;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.CheckSgcException;
import org.esupportail.esupsgcclient.service.EncodingException;
import org.esupportail.esupsgcclient.service.EncodingService;
import org.esupportail.esupsgcclient.service.cnous.CnousFournisseurCarteException;
import org.esupportail.esupsgcclient.service.pcsc.PcscException;
import org.esupportail.esupsgcclient.service.webcam.WebcamQRCodeReader;
import org.esupportail.esupsgcclient.ui.EsupSGCJFrame;
import org.esupportail.esupsgcclient.utils.Utils;

import com.github.sarxos.webcam.WebcamException;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

public class EsupSGCClientApplication {

	private final static Logger log = Logger.getLogger(EsupSGCClientApplication.class);

	private static WebcamQRCodeReader webcamQRCodeReader;
	private static EncodingService encodingService = new EncodingService();
	private static EsupSGCJFrame esupSGCJFrame;
	private static Color GREEN = new Color(3, 193, 0);
	private static Color ORANGE = new Color(226, 128, 0);
	private static boolean isClientReady = false; 

	public static void main(String... args) {

		try {
			esupSGCJFrame = new EsupSGCJFrame();
			esupSGCJFrame.changeTextPrincipal("Chargement...", Color.BLACK);
			addJframeListerners();
			esupSGCJFrame.initSteps();

			encodingService.init(args);
			esupSGCJFrame.addLogTextLn("INFO", "pc/sc : OK");
			
			try {
				String path1 = encodingService.sgcUrl + "/resources/images/logo1.png";
				URL url1 = new URL(path1);
				BufferedImage bufImg1 = ImageIO.read(url1);
				esupSGCJFrame.image1JPanel.setIcon(new ImageIcon(bufImg1));
				String path2 = encodingService.sgcUrl + "/resources/images/logo2.png";
				URL url2 = new URL(path2);
				BufferedImage bufImg2 = ImageIO.read(url2);
				esupSGCJFrame.image2JPanel.setIcon(new ImageIcon(bufImg2));
			}catch (Exception e){
				log.warn("logo display error", e);
			}
			
			esupSGCJFrame.stepClientReady.setForeground(ORANGE);
			esupSGCJFrame.initWebCam();
			esupSGCJFrame.addLogTextLn("INFO", "webcam : OK");
			
			if(encodingService.encodeCnous){			
				esupSGCJFrame.addLogTextLn("INFO", "dll cnous : OK");				
			}else{
				esupSGCJFrame.stepEncodageCnous.setVisible(false);
				esupSGCJFrame.stepSendCSV.setVisible(false);
			}
			
			if (esupSGCJFrame.webcam != null) {
				webcamQRCodeReader = new WebcamQRCodeReader(encodingService.eppnInit, encodingService.sgcUrl,
						esupSGCJFrame);
				esupSGCJFrame.addLogTextLn("INFO", "numeroId = " + encodingService.numeroId);
				esupSGCJFrame.addLogTextLn("INFO", "eppnInit = " + encodingService.eppnInit);
				esupSGCJFrame.addLogTextLn("INFO", "esupNfcTagServerUrl = " + encodingService.esupNfcTagServerUrl);
				esupSGCJFrame.addLogTextLn("INFO", "sgcUrl = " + encodingService.sgcUrl);
				
				isClientReady = true;
				
			} else {
				log.warn("webcam : Failed");
				esupSGCJFrame.addLogTextLn("ERROR", "webcam : Failed");
				esupSGCJFrame.changeTextPrincipal("Erreur webcam", Color.RED);
			}
			
		}catch (CnousFournisseurCarteException e) {
			esupSGCJFrame.addLogTextLn("ERROR", "dll cnous : Failed");
			esupSGCJFrame.stepClientReady.setForeground(Color.RED);
			esupSGCJFrame.stepClientReady.setText("Client non prêt");
			esupSGCJFrame.changeTextPrincipal("Erreur de configuration cnous", Color.RED);
			esupSGCJFrame.addLogTextLn("ERROR", getExceptionString(e));
			playSound("fail.wav");
			log.error("Erreur de configuration cnous", e);
		}catch (EncodingException | PcscException e) {
			esupSGCJFrame.addLogTextLn("ERROR", "pc/sc : Failed");
			esupSGCJFrame.stepClientReady.setForeground(Color.RED);
			esupSGCJFrame.stepClientReady.setText("Client non prêt");
			esupSGCJFrame.changeTextPrincipal("Erreur lecteur PC/SC", Color.RED);
			esupSGCJFrame.addLogTextLn("ERROR", getExceptionString(e));
			playSound("fail.wav");
			log.error("Erreur lecteur PC/SC", e);
		}catch (WebcamException e) {
			esupSGCJFrame.addLogTextLn("ERROR", "webcam : Failed");
			esupSGCJFrame.stepClientReady.setForeground(Color.RED);
			esupSGCJFrame.stepClientReady.setText("Client non prêt");
			esupSGCJFrame.changeTextPrincipal("Erreur de webcam, voir les logs", Color.RED);
			esupSGCJFrame.addLogTextLn("ERROR", getExceptionString(e));
			playSound("fail.wav");
			log.error("Erreur de webcam", e);
		} catch (Exception e) {
			esupSGCJFrame.addLogTextLn("ERROR", "erreur inconue");
			esupSGCJFrame.stepClientReady.setForeground(Color.RED);
			esupSGCJFrame.stepClientReady.setText("Client non prêt");
			esupSGCJFrame.changeTextPrincipal("Erreur inconnue, voir les logs", Color.RED);
			esupSGCJFrame.addLogTextLn("ERROR", getExceptionString(e));
			playSound("fail.wav");
			log.error("Erreur inconnue", e);
		}

		if(isClientReady) {
			run();
		}
		
	}

	private static void run(){
		while (true) {
			esupSGCJFrame.initSteps();
			esupSGCJFrame.stepClientReady.setText("Client prêt");
			esupSGCJFrame.stepClientReady.setForeground(GREEN);
			esupSGCJFrame.stepReadQR.setForeground(ORANGE);
			esupSGCJFrame.changeTextPrincipal("En attente d'une carte...", ORANGE);
			String qrcode = webcamQRCodeReader.readQrCode();
			if (qrcode != null) {
				esupSGCJFrame.changeTextPrincipal("Traitement en cours...", ORANGE);
				esupSGCJFrame.stepReadQR.setForeground(GREEN);
				esupSGCJFrame.addLogTextLn("INFO", qrcode + " detected");
				log.info("qrcode detected : " + qrcode);
				esupSGCJFrame.stepReadCSN.setForeground(ORANGE);
				String csn = "";
				try {
					Utils.sleep(1000);
					encodingService.pcscConnection();
					csn = encodingService.readCsn();
					esupSGCJFrame.stepReadCSN.setForeground(GREEN);
					esupSGCJFrame.addLogTextLn("INFO", "csn : " + csn);

					esupSGCJFrame.stepSelectSGC.setForeground(ORANGE);
					encodingService.checkBeforeEncoding(qrcode, csn);
					esupSGCJFrame.stepSelectSGC.setForeground(GREEN);
					esupSGCJFrame.addLogTextLn("INFO", qrcode + " checked in SGC");

					esupSGCJFrame.stepEncodageApp.setForeground(ORANGE);
					esupSGCJFrame.addLogTextLn("INFO", "Encoding : Start");
					String encodingResult = encodingService.appsEncoding(csn, esupSGCJFrame);

					if ("END".equals(encodingResult)) {
						log.info("encoding ok for : " + qrcode + " - csn : " + csn);
						esupSGCJFrame.addLogTextLn("INFO", "Encoding :  OK");
						esupSGCJFrame.stepEncodageApp.setForeground(GREEN);

						encodingService.pcscDisconnect();

						if (encodingService.isCnousOK() && encodingService.encodeCnous) {
							esupSGCJFrame.stepEncodageCnous.setForeground(ORANGE);
							esupSGCJFrame.addLogTextLn("INFO", "Cnous Encoding :  Start");
							encodingService.delCnousCsv();
							if (encodingService.cnousEncoding(csn)) {
								esupSGCJFrame.stepEncodageCnous.setForeground(GREEN);
								esupSGCJFrame.addLogTextLn("INFO", "Cnous Encoding :  OK");
								log.info("cnous encoding : OK");
								esupSGCJFrame.stepSendCSV.setForeground(ORANGE);
								esupSGCJFrame.addLogTextLn("INFO", "Cnous csv start :  OK");
								if (encodingService.sendCnousCsv(csn)) {
									esupSGCJFrame.stepSendCSV.setForeground(GREEN);
									esupSGCJFrame.addLogTextLn("INFO", "Cnous csv send :  OK");
									log.info("cnous csv send : OK");
									esupSGCJFrame.changeTextPrincipal("Encodage terminé", GREEN);
									playSound("success.wav");
								} else {
									esupSGCJFrame.stepSendCSV.setForeground(Color.RED);
									esupSGCJFrame.addLogTextLn("INFO", "Cnous csv send :  Failed");
									log.error("cnous csv send : Failed");
								}
							} else {
								esupSGCJFrame.stepEncodageCnous.setForeground(Color.RED);
								esupSGCJFrame.addLogTextLn("INFO", "Cnous Encoding :  Failed");
								log.error("cnous csv send : Failed for qrcode " + qrcode + ", csn " + csn);
							}
						} else {
							esupSGCJFrame.addLogTextLn("INFO", "Cnous Encoding :  Skipped");
							esupSGCJFrame.addLogTextLn("INFO", "change card");
							esupSGCJFrame.changeTextPrincipal("Encodage terminé", GREEN);
							playSound("success.wav");
						}

					} else {
						esupSGCJFrame.stepEncodageApp.setForeground(Color.RED);
						esupSGCJFrame.addLogTextLn("ERROR", "Nothing to do - message from server : " + encodingResult);
						log.warn("Nothing to do - message from server : " + encodingResult);
						esupSGCJFrame.changeTextPrincipal("Erreur d'encodage, voir les logs",Color.RED);
					}
				} catch (PcscException e) {
					log.warn("pcsc error for : " + qrcode);
					esupSGCJFrame.stepReadCSN.setForeground(Color.RED);
					esupSGCJFrame.changeTextPrincipal("Erreur lecteur de carte, voir les logs", Color.RED);
					esupSGCJFrame.addLogTextLn("ERROR", "pcsc error : " + getExceptionString(e));
					Utils.sleep(3000);
					playSound("fail.wav");
					continue;
				} catch (EncodingException e) {
					log.warn("encoding error for : " + qrcode + " - csn : " + csn);
					esupSGCJFrame.stepEncodageApp.setForeground(Color.RED);
					esupSGCJFrame.addLogTextLn("ERROR", "encoding error : " + getExceptionString(e));
					esupSGCJFrame.changeTextPrincipal("Erreur d'encodage, voir les logs",Color.RED);
					playSound("fail.wav");
					Utils.sleep(3000);
				} catch (CnousFournisseurCarteException e) {
					esupSGCJFrame.stepEncodageCnous.setForeground(Color.RED);
					esupSGCJFrame.addLogTextLn("ERROR", "cnous encoding error : " + e.getMessage());
					esupSGCJFrame.changeTextPrincipal("Erreur CROUS, voir les logs", Color.RED);
					playSound("fail.wav");
				} catch (CheckSgcException e) {
					esupSGCJFrame.stepSelectSGC.setForeground(Color.RED);
					esupSGCJFrame.addLogTextLn("ERROR", "sgc error : " + getExceptionString(e));
					esupSGCJFrame.changeTextPrincipal("Erreur SGC, voir les logs", Color.RED);
					playSound("fail.wav");
					Utils.sleep(3000);
				} 
			}
			while (!encodingService.pcscCardOnTerminal()){
				Utils.sleep(1000);
			}
			esupSGCJFrame.addLogTextLn("INFO", "Ready");
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
	
	private static String getExceptionString(Throwable throwable){
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		return sw.toString();
	}

	private static void exit() {
		esupSGCJFrame.exit();
		System.gc();
		System.exit(0);
	}

	private static void playSound(String soundFile) {
		try {
			InputStream in = EsupSGCClientApplication.class.getResourceAsStream("/sound/"+soundFile);
		    AudioStream audioStream = new AudioStream(in);
		    AudioPlayer.player.start(audioStream);
		} catch (IOException e) {
			log.warn("error play sound", e);
		}
	}
	
}
