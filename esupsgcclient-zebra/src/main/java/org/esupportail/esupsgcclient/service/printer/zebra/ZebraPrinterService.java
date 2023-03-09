package org.esupportail.esupsgcclient.service.printer.zebra;

import java.awt.*;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashMap;
import java.util.List;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.common.card.comm.internal.CardError;
import com.zebra.sdk.common.card.containers.GraphicsInfo;
import com.zebra.sdk.common.card.containers.PrinterStatusInfo;
import com.zebra.sdk.common.card.enumerations.CardDestination;
import com.zebra.sdk.common.card.enumerations.CardSide;
import com.zebra.sdk.common.card.enumerations.CardSource;
import com.zebra.sdk.common.card.enumerations.GraphicType;
import com.zebra.sdk.common.card.enumerations.OrientationType;
import com.zebra.sdk.common.card.enumerations.PrintType;
import com.zebra.sdk.common.card.enumerations.SmartCardEncoderType;
import com.zebra.sdk.common.card.graphics.ZebraCardGraphics;
import com.zebra.sdk.common.card.graphics.ZebraCardImageI;
import com.zebra.sdk.common.card.graphics.ZebraGraphics;
import com.zebra.sdk.common.card.graphics.enumerations.RotationType;
import com.zebra.sdk.common.card.settings.ZebraCardSettingNames;
import com.zebra.sdk.zmotif.printer.internal.ZmotifPrinterImpl;
import com.zebra.sdk.zxp.comm.internal.ZXPBase;
import com.zebra.sdk.zxp.comm.internal.ZXPPrn;
import com.zebra.sdk.zxp.device.internal.ZxpDevice;
import com.zebra.sdk.zxp.printer.internal.ZxpPrinterImpl;
import com.zebra.sdk.zxp.printer.internal.ZxpZebraPrinter;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.Tooltip;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;

import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.ui.EsupSgcTestPcscDialog;
import org.esupportail.esupsgcclient.utils.Utils;

import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.containers.JobStatus;
import com.zebra.sdk.common.card.containers.JobStatusInfo;
import com.zebra.sdk.common.card.errors.ZebraCardErrors;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.jobSettings.ZebraCardJobSettingNames;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory;
import com.zebra.sdk.printer.discovery.DiscoveredUsbPrinter;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;
import com.zebra.sdk.settings.SettingsException;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class ZebraPrinterService extends EsupSgcPrinterService {
	
	private final static Logger log = Logger.getLogger(ZebraPrinterService.class);
	
	private static Integer CARD_FEED_TIMEOUT = 30000;

	@Resource
	ZebraHeartbeatTaskService zebraHeartbeatTaskService;

	@Resource
	EsupSgcTestPcscDialog esupSgcTestPcscDialog;

	@Resource
	AppConfig appConfig;

	ZebraCardPrinter zebraCardPrinter;
	int jobId;

	TextArea logTextarea;

	@Override
	public void setupJfxUi(Tooltip tooltip, TextArea logTextarea, MenuBar menuBar) {
		this.logTextarea = logTextarea;
		tooltip.textProperty().bind(zebraHeartbeatTaskService.titleProperty());
		zebraHeartbeatTaskService.start();
		zebraHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(newValue + "\n")));

		MenuItem zebraReject = new MenuItem();
		zebraReject.setText("Rejeter la carte");
		MenuItem zebraPrintEnd = new MenuItem();
		zebraPrintEnd.setText("Clore la session d'impression");
		MenuItem testPcsc = new MenuItem();
		testPcsc.setText("Stress test pc/sc");
		MenuItem reconnect = new MenuItem();
		reconnect.setText("Reconnexion de l'imprimante");
		Menu zebraMenu = new Menu();
		zebraMenu.setText("Zebra");
		zebraMenu.getItems().addAll(zebraReject, zebraPrintEnd, testPcsc, reconnect);
		menuBar.getMenus().add(zebraMenu);

		zebraPrintEnd.setOnAction(actionEvent -> {
			new Thread(() -> {
				try {
					cancelJobs();
					Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Cancels Jobs OK \n"));
				} catch (ConnectionException | ZebraCardException e) {
					Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(e.getMessage() + "\n"));
					throw new RuntimeException(e);
				}
			}).start();
		});

		zebraReject.setOnAction(actionEvent -> {
			new Thread(() -> {
				eject();
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Eject OK \n"));
			}).start();
		});

		reconnect.setOnAction(actionEvent -> {
			new Thread(() -> {init();}).start();
		});

		testPcsc.setOnAction(actionEvent -> {
			esupSgcTestPcscDialog.getTestPcscDialog(
					()-> {
						try {
							launchEncoding();
						} catch (SettingsException | ConnectionException | ZebraCardException e) {
							throw new RuntimeException("Pb when launching zebra encoding", e);
						}
					},
					()->{
						eject();
					}
			).show();
		});
	}
	
	public synchronized void init() {
		log.info("Zebra init connection ...");
		while(zebraCardPrinter == null) {
			try {
				DiscoveredUsbPrinter[] discoveredPrinters;
				discoveredPrinters = UsbDiscoverer.getZebraUsbPrinters();
				for (DiscoveredUsbPrinter discoveredPrinter : discoveredPrinters) {
					log.info("Discover Zebra printer ...");
					Connection connection = discoveredPrinter.getConnection();
						if (!connection.isConnected()) {
							log.info("zebra not connected - try to connect");
							connection.open();
						}
						zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection);
						log.info("Zebra connection OK");
						break;
				}
				if(zebraCardPrinter == null) {
					log.warn("Cant connect Zebra printer, retry in 3 sec");
					Utils.sleep(2000);
				}
			} catch (Exception e) {
				log.error("Zebra init error", e);
			}
		}

		try {
			log.info("Settings range of offset : " + zebraCardPrinter.getSettingRange(ZebraCardSettingNames.SMARTCARD_X_OFFSET));
			log.info("Settings range of internal encoder contactless : " + zebraCardPrinter.getSettingRange(ZebraCardSettingNames.INTERNAL_ENCODER_CONTACTLESS_ENCODER));
			log.info(String.format("Printer cards count : %s", zebraCardPrinter.getCardCount().totalCards));
			log.info(String.format("Printer sensor states : %s", zebraCardPrinter.getSensorStates()));
			String logText = String.format("Printer Firmware : %s", zebraCardPrinter.getPrinterInformation().firmwareVersion) + "\n" +
					"Settings range of encoder contactless for printerZebraEncoderType property on esup-sgc-client config : " + zebraCardPrinter.getJobSettingRange(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS) + "\n" +
					String.format("printerZebraEncoderType : %s", appConfig.getPrinterZebraEncoderType()) + "\n";
			log.info(logText);
			Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(logText));
		} catch (Exception e) {
			log.warn("Pb getting zebra settings", e);
		}

	}
	void setSmartcardJob() throws SettingsException {
		zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_SOURCE, CardSource.Feeder.name());
		zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_DESTINATION, CardDestination.Hold.name());
		String smartCardEncodeTypeSetup = appConfig.getPrinterZebraEncoderType();
		if(StringUtils.isEmpty(smartCardEncodeTypeSetup)) {
			String  smartCardEncodeTypeRange = zebraCardPrinter.getJobSettingRange(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS);
			for(String smartCardEncodeType : Arrays.asList(SmartCardEncoderType.MIFARE.toString().toLowerCase(), SmartCardEncoderType.UHF.toString().toLowerCase(), "hf", SmartCardEncoderType.Other.toString().toLowerCase())) {
				if(smartCardEncodeTypeRange.contains(smartCardEncodeType)) {
					smartCardEncodeTypeSetup = smartCardEncodeType;
					break;
				}
			}
		}
		log.debug(String.format("zebra smarcard job setup with smartCardEncodeType to %s", smartCardEncodeTypeSetup));
		zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS, smartCardEncodeTypeSetup);
	}

	public void launchEncoding() throws SettingsException, ConnectionException, ZebraCardException {
		setSmartcardJob();
		jobId = zebraCardPrinter.smartCardEncode(1);
		pollJobStatus();
		if(zebraCardPrinter instanceof ZxpZebraPrinter && appConfig.getPrinterZebraHackZxpNfcPower()) {
			// Hack for ZXP3 and SDI010 USB Smart Card Reader on linux to power on nfc reader ?!
			log.info("HackZxpNfcPower is on ... unloadContactSmartcard so that nfc reader power is on ?!");
			ZxpDevice zxpDevice = new ZxpDevice(zebraCardPrinter.getConnection());
			ZXPPrn zxpPrn = zxpDevice.getZxpPrinter();
			ZXPBase.Response res = new ZXPBase.Response();
			CardError cardError = new CardError();
			zxpPrn.unloadContactSmartcard(res, cardError);
		}
	}

	boolean pollJobStatus(){
		boolean done = false;
		long start = System.currentTimeMillis();

		log.debug("Polling status for job id " + jobId + "...\n");
		
		while(!done) {
			Utils.sleep(250);
			JobStatusInfo jobStatus = null;
			try {
				jobStatus = zebraCardPrinter.getJobStatus(jobId);
			} catch (ConnectionException | ZebraCardException e) {
				log.error("zebra : get job status",e);
				return false;
			}
	
			String alarmDesc = jobStatus.alarmInfo.value > 0 ? " (" + jobStatus.alarmInfo.description + ")" : "";
			String errorDesc = jobStatus.errorInfo.value > 0 ? " (" + jobStatus.errorInfo.description + ")" : "";
	
			log.debug(String.format("Job %d: status:%s, position:%s, contact:%s, contactless:%s, alarm:%d%s, error:%d%s%n", jobId, jobStatus.printStatus, jobStatus.cardPosition,
					jobStatus.contactSmartCard, jobStatus.contactlessSmartCard, jobStatus.alarmInfo.value, alarmDesc, jobStatus.errorInfo.value, errorDesc));
			if (jobStatus.printStatus.contains("done_ok") || jobStatus.contactlessSmartCard.contains("at_station")) {
				done = true;
			} else if (jobStatus.printStatus.contains("cancelled_by_user") ) {
				try {
					cancelJobs();
				} catch (ConnectionException | ZebraCardException e) {
					log.debug("cancel job failed");
				}
				return false;
			} else if (jobStatus.printStatus.contains("error") || jobStatus.printStatus.contains("cancelled")) {
				log.debug("Zebra job error");
			} else if (jobStatus.errorInfo.value > 0) {
				log.debug("The job encountered an error [" + jobStatus.errorInfo.description + "] and was cancelled.");
				break;
			} else if (jobStatus.alarmInfo.value > 0) {
				log.debug("Zebra alarm : " + jobStatus.alarmInfo.value);
			} else if ((jobStatus.printStatus.contains("in_progress") && jobStatus.cardPosition.contains("feeding")) // ZMotif printers
					|| (jobStatus.printStatus.contains("alarm_handling") && jobStatus.alarmInfo.value == ZebraCardErrors.MEDIA_OUT_OF_CARDS)) { // ZXP printers
				if (System.currentTimeMillis() > start + CARD_FEED_TIMEOUT) {
					log.warn("Card feed time out");
				}
			}
		}
		return done;
	}

	public void cancelJob(){
		try {
			zebraCardPrinter.cancel(jobId);
			log.info("Job ID " + jobId + " was cancelled.%n");
		} catch (ConnectionException | ZebraCardException e) {
			log.error(e);
		}
	}
	
	public void cancelJobs() throws ConnectionException, ZebraCardException {
		if(zebraCardPrinter != null) {
			List<JobStatus> jobs = zebraCardPrinter.getJobList();
			for(JobStatus job : jobs) {
				zebraCardPrinter.cancel(job.jobId);
			}
		} else {
			throw new ZebraCardException("zebraCardPrinter is null - try reset printer");
		}
	}

	public String getStatus() {
		String status = null;
		if(zebraCardPrinter != null) {
			while(status == null){
				Utils.sleep(250);
				try {
					PrinterStatusInfo printerStatusInfo = zebraCardPrinter.getPrinterStatus();
					status = String.format("%s %s", printerStatusInfo.status, printerStatusInfo.alarmInfo.description);
					log.trace(status);
				} catch (Exception e) {
					log.error(e);
				}
			}
		}
		return status;
	}
	
	public String getStatusMessage(String status) {
		if(status.toLowerCase().contains("out of cards")) {
			return "Chargeur vide";
		}else if(status.toLowerCase().contains("jam")) {
			return "Erreur mécanique";
		}else {
			return "En attente de redémarrage";
		}
		
	}

	@Override
	public String getMaintenanceInfo() throws Exception {
		return zebraCardPrinter.getCardCount().cardCounterInfo.toString();
	}



	public void eject() {
		try {
			zebraCardPrinter.ejectCard();
		} catch (ConnectionException | ZebraCardException e) {
			throw new RuntimeException(e);
		}
	}

	public void print(String bmpBlackAsBase64, String bmpColorAsBase64, String bmpOverlayAsBase64) {
		try {
			List<GraphicsInfo> graphicsData = new ArrayList<GraphicsInfo>();
			graphicsData.add(drawImage(bmpColorAsBase64, PrintType.Color));
			graphicsData.add(drawImage(bmpBlackAsBase64, PrintType.MonoK));
			graphicsData.add(drawImage(bmpOverlayAsBase64, PrintType.Overlay));
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_SOURCE, CardSource.Feeder.name());
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_DESTINATION, CardDestination.Hold.name());
			jobId = zebraCardPrinter.print(1, graphicsData);
			pollJobStatus();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private GraphicsInfo drawImage(String imageData, PrintType printType) throws IOException, ConnectionException, ZebraCardException {
		ZebraGraphics graphics = new ZebraCardGraphics(zebraCardPrinter);
		graphics.initialize(0, 0, OrientationType.Landscape, printType, Color.WHITE);
		graphics.drawImage(Base64.getDecoder().decode(imageData), 0, 0, 0, 0, RotationType.RotateNoneFlipNone);
		ZebraCardImageI zebraCardImage = graphics.createImage();
		GraphicsInfo graphicsInfo = new GraphicsInfo();
		graphicsInfo.side = CardSide.Front;
		graphicsInfo.fillColor = -1;
		graphicsInfo.graphicData = zebraCardImage;
		graphicsInfo.graphicType = GraphicType.BMP;
		graphicsInfo.printType = printType;
		return graphicsInfo;
	}

	public void reject() {
		if(zebraCardPrinter instanceof ZxpZebraPrinter) {
			try {
				ZxpDevice zxpDevice = new ZxpDevice(zebraCardPrinter.getConnection());
				ZXPPrn zxpPrn = zxpDevice.getZxpPrinter();
				zxpPrn.flipCard(new ZXPBase.Response(), new CardError());
			} catch (ConnectionException e) {
				log.error("Exception when flip card", e);
				return;
			}
		}
		this.eject();
	}
}
