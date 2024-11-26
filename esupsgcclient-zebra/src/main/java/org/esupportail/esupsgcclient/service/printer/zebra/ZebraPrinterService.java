package org.esupportail.esupsgcclient.service.printer.zebra;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.comm.internal.CardError;
import com.zebra.sdk.common.card.containers.GraphicsInfo;
import com.zebra.sdk.common.card.containers.JobStatus;
import com.zebra.sdk.common.card.containers.JobStatusInfo;
import com.zebra.sdk.common.card.containers.PrinterStatusInfo;
import com.zebra.sdk.common.card.enumerations.CardDestination;
import com.zebra.sdk.common.card.enumerations.CardSide;
import com.zebra.sdk.common.card.enumerations.CardSource;
import com.zebra.sdk.common.card.enumerations.GraphicType;
import com.zebra.sdk.common.card.enumerations.OrientationType;
import com.zebra.sdk.common.card.enumerations.PrintType;
import com.zebra.sdk.common.card.enumerations.SmartCardEncoderType;
import com.zebra.sdk.common.card.errors.ZebraCardErrors;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.graphics.ZebraCardGraphics;
import com.zebra.sdk.common.card.graphics.ZebraCardImageI;
import com.zebra.sdk.common.card.graphics.ZebraGraphics;
import com.zebra.sdk.common.card.graphics.enumerations.RotationType;
import com.zebra.sdk.common.card.jobSettings.ZebraCardJobSettingNames;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory;
import com.zebra.sdk.common.card.settings.ZebraCardSettingNames;
import com.zebra.sdk.printer.discovery.DiscoveredUsbPrinter;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;
import com.zebra.sdk.settings.SettingsException;
import com.zebra.sdk.zxp.comm.internal.ZXPBase;
import com.zebra.sdk.zxp.comm.internal.ZXPPrn;
import com.zebra.sdk.zxp.device.internal.ZxpDevice;
import com.zebra.sdk.zxp.printer.internal.ZxpZebraPrinter;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.TilePane;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.hack.zebra.ForceLoadDll;
import org.esupportail.esupsgcclient.hack.zebra.UsbConnection;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.ui.EsupSgcDesfireFullTestPcscDialog;
import org.esupportail.esupsgcclient.ui.EsupSgcTestPcscDialog;
import org.esupportail.esupsgcclient.ui.LogTextAreaService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.awt.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;

@Component
public class ZebraPrinterService extends EsupSgcPrinterService {
	
	private final static Logger log = LoggerFactory.getLogger(ZebraPrinterService.class);
	
	private static Integer CARD_FEED_TIMEOUT = 30000;

	@Resource
	ZebraHeartbeatTaskService zebraHeartbeatTaskService;

	@Resource
	EsupSgcTestPcscDialog esupSgcTestPcscDialog;

	@Resource
	EsupSgcDesfireFullTestPcscDialog esupSgcDesfireFullTestPcscDialog;

	@Resource
	AppConfig appConfig;

	ZebraCardPrinter zebraCardPrinter;
	int jobId;

	@Resource
	LogTextAreaService logTextAreaService;

	@Value("${printerZebraHackZxp3HalfBug:false}")
	Boolean hackZxp3HalfBug;

	@Value("${printerZebraRotation:false}")
	Boolean printerZebraRotation;

	static {
		ForceLoadDll.loadDll();
	}

	@Override
	public void setupJfxUi(Stage stage, Tooltip tooltip, MenuBar menuBar) {
		tooltip.textProperty().bind(zebraHeartbeatTaskService.titleProperty());
		zebraHeartbeatTaskService.start();
		zebraHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextAreaService.appendText(newValue));

		MenuItem zebraReject = new MenuItem();
		zebraReject.setText("Rejeter la carte");
		MenuItem zebraPrintEnd = new MenuItem();
		zebraPrintEnd.setText("Clore la session d'impression");
		MenuItem testPcsc = new MenuItem();
		testPcsc.setText("Stress test pc/sc");
		MenuItem pcscDesfireTest = new MenuItem();
		pcscDesfireTest.setText("Stress test PC/SC DES Blank Desfire");
		MenuItem reconnect = new MenuItem();
		reconnect.setText("Reconnexion de l'imprimante");
		MenuItem updateFirmware = new MenuItem();
		updateFirmware.setText("Mise a jour du firmware de l'imprimante");
		MenuItem zebraCommand = new MenuItem();
		zebraCommand.setText("Envoyer une commande avancée à l'imprimante");
		Menu zebraMenu = new Menu();
		zebraMenu.setText("Zebra");
		zebraMenu.getItems().addAll(zebraReject, zebraPrintEnd, testPcsc, pcscDesfireTest, reconnect, updateFirmware, zebraCommand);
		menuBar.getMenus().add(zebraMenu);

		zebraPrintEnd.setOnAction(actionEvent -> {
			new Thread(() -> {
				try {
					cancelJobs();
					logTextAreaService.appendText("Cancels Jobs OK");
				} catch (ConnectionException | ZebraCardException e) {
					logTextAreaService.appendText(e.getMessage());
					throw new RuntimeException(e);
				}
			}).start();
		});

		zebraReject.setOnAction(actionEvent -> {
			new Thread(() -> {
				eject();
				logTextAreaService.appendText("Eject OK");
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

		pcscDesfireTest.setOnAction(actionEvent -> {
			esupSgcDesfireFullTestPcscDialog.getTestPcscDialog(
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

		final FileChooser fileChooser = new FileChooser();
		updateFirmware.setOnAction(actionEvent -> {
			File file = fileChooser.showOpenDialog(stage);
			if (file != null) {
				try {
					logTextAreaService.appendText(String.format("Envoi du fichier %s pour mise à jour du firmaware de l'imprimante zebra en cours ...", file.getAbsolutePath()));
					zebraCardPrinter.updateFirmware(file.getAbsolutePath());
					logTextAreaService.appendText("Mise à jour réussie !");
					logTextAreaService.appendText("Merci de redémarrer l'application.");
				} catch (Exception e) {
					logTextAreaService.appendText(String.format("Mise à jour échouée ... : %s", e.getMessage()));
					log.error("Exception lors de la mise à jour du firmware Zebra", e);
				}
			}
		});

		TilePane r = new TilePane();
		TextInputDialog td = new TextInputDialog("+OS 0");
		td.setHeaderText("Envoyer une commande avancée à l'imprimante");
		td.setContentText("Commande");
		zebraCommand.setOnAction(actionEvent -> {
			Optional<String> result = td.showAndWait();
			result.ifPresent(command -> {
				new Thread(() -> {
					try {
						logTextAreaService.appendText(String.format("Commande zebra : %s", command));
							Connection c = zebraCardPrinter.getConnection();
							if(!c.isConnected()) {
								c.open();
							}
							String zebraResponse = "";
							c.write(("\u001B" + command + "\r").getBytes("ISO-8859-1"));
					} catch (Exception e) {
						logTextAreaService.appendText(String.format("Exception zebra : %s", e.getMessage()));
						log.error("Exception zebra lors de la commande " + command, e);
					}
				}).start();
			});
		});

	}
	
	public synchronized void init() {
		log.info("Zebra init connection ...");
		if(zebraCardPrinter!=null) {
			try {
				zebraCardPrinter.getConnection().close();
			} catch (ConnectionException e) {
				log.error("Zebra close error", e);
			}
		}
		zebraCardPrinter = null;
		while(zebraCardPrinter == null) {
			try {
				DiscoveredUsbPrinter[] discoveredPrinters;
				discoveredPrinters = UsbDiscoverer.getZebraUsbPrinters();
				for (DiscoveredUsbPrinter discoveredPrinter : discoveredPrinters) {
					log.info("Discover Zebra printer ...");
					Connection connection = null;
					if(hackZxp3HalfBug) {
						// Hack ZXP3 - debug command error with half command
						connection = new UsbConnection(discoveredPrinter.address);
					} else {
						connection = discoveredPrinter.getConnection();
					}
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
			if(zebraCardPrinter.getAvailableSettings().contains(ZebraCardSettingNames.INTERNAL_ENCODER_CONTACTLESS_ENCODER)) {
				log.info("Settings range of internal encoder contactless : " + zebraCardPrinter.getSettingRange(ZebraCardSettingNames.INTERNAL_ENCODER_CONTACTLESS_ENCODER));
			}
			log.info(String.format("Printer cards count : %s", zebraCardPrinter.getCardCount().totalCards));
			log.info(String.format("Printer sensor states : %s", zebraCardPrinter.getSensorStates()));
			String logText = String.format("Printer Firmware : %s", zebraCardPrinter.getPrinterInformation().firmwareVersion) + "\n" +
					(zebraCardPrinter.getJobSettings().contains(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS) ?
							("Settings range of encoder contactless for printerZebraEncoderType property on esup-sgc-client config : " + zebraCardPrinter.getJobSettingRange(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS) + "\n") : "") +
					String.format("printerZebraEncoderType : %s", appConfig.getPrinterZebraEncoderType()) + "\n" +
					String.format("Smart Card Offset : %s", zebraCardPrinter.getSettingValue(ZebraCardSettingNames.SMARTCARD_X_OFFSET));
			log.info(logText);
			logTextAreaService.appendText(logText);
		} catch (Exception e) {
			log.warn("Pb getting zebra settings", e);
		}

	}
	void setSmartcardJob() throws SettingsException {
		zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_SOURCE, CardSource.Feeder.name());
		zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_DESTINATION, CardDestination.Hold.name());
		if(zebraCardPrinter.getJobSettings().contains(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS)) {
			String smartCardEncodeTypeSetup = appConfig.getPrinterZebraEncoderType();
			if (StringUtils.isEmpty(smartCardEncodeTypeSetup)) {
				String smartCardEncodeTypeRange = zebraCardPrinter.getJobSettingRange(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS);
				for (String smartCardEncodeType : Arrays.asList(SmartCardEncoderType.MIFARE.toString().toLowerCase(), SmartCardEncoderType.UHF.toString().toLowerCase(), "hf", SmartCardEncoderType.Other.toString().toLowerCase())) {
					if (smartCardEncodeTypeRange.contains(smartCardEncodeType)) {
						smartCardEncodeTypeSetup = smartCardEncodeType;
						break;
					}
				}
			}
			log.debug(String.format("zebra smarcard job setup with smartCardEncodeType to %s", smartCardEncodeTypeSetup));
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS, smartCardEncodeTypeSetup);
		} else {
			log.debug(String.format("No native support of nfc on this zebra ?"));
		}
	}

	public void launchEncoding() throws SettingsException, ConnectionException, ZebraCardException {
		setSmartcardJob();
		if(zebraCardPrinter.getJobSettings().contains(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS)) {
			jobId = zebraCardPrinter.smartCardEncode(1);
		} else {
			jobId = zebraCardPrinter.positionCard();
		}
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

	boolean pollJobStatus() throws ZebraCardException {
		boolean done = false;
		long start = System.currentTimeMillis();

		log.debug("Polling status for job id " + jobId + "...\n");
		
		while(!done) {
			Utils.sleep(250);
			JobStatusInfo jobStatus = null;
			try {
				jobStatus = zebraCardPrinter.getJobStatus(jobId);
			} catch (ConnectionException | ZebraCardException e) {
				throw new ZebraCardException("zebra : get job status", e);
			}
	
			String alarmDesc = jobStatus.alarmInfo.value > 0 ? " (" + jobStatus.alarmInfo.description + ")" : "";
			String errorDesc = jobStatus.errorInfo.value > 0 ? " (" + jobStatus.errorInfo.description + ")" : "";
	
			log.trace(String.format("Job %d: status:%s, position:%s, contact:%s, contactless:%s, alarm:%d%s, error:%d%s%n", jobId, jobStatus.printStatus, jobStatus.cardPosition,
					jobStatus.contactSmartCard, jobStatus.contactlessSmartCard, jobStatus.alarmInfo.value, alarmDesc, jobStatus.errorInfo.value, errorDesc));
			if (jobStatus.printStatus.contains("done_ok") || jobStatus.contactlessSmartCard.contains("at_station")) {
				done = true;
			} else if (jobStatus.printStatus.contains("cancelled_by_user") ) {
				try {
					cancelJobs();
				} catch (ConnectionException | ZebraCardException e) {
					log.debug("cancel job failed");
				}
				throw new ZebraCardException("cancelled_by_user");
			} else if (jobStatus.printStatus.contains("error") || jobStatus.printStatus.contains("cancelled")) {
				throw new ZebraCardException("Zebra job error" + jobStatus.printStatus);
			} else if (jobStatus.errorInfo.value > 0) {
				throw new ZebraCardException("The job encountered an error [" + jobStatus.errorInfo.description + "] and was cancelled.");
			} else if (jobStatus.alarmInfo.value > 0) {
				log.warn("Zebra alarm : " + jobStatus.alarmInfo.value);
			} else if ((jobStatus.printStatus.contains("in_progress") && jobStatus.cardPosition.contains("feeding")) // ZMotif printers
					|| (jobStatus.printStatus.contains("alarm_handling") && jobStatus.alarmInfo.value == ZebraCardErrors.MEDIA_OUT_OF_CARDS)) { // ZXP printers
				if (System.currentTimeMillis() > start + CARD_FEED_TIMEOUT) {
					throw new ZebraCardException("Card feed time out");
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
			log.error("Exception on ZebraPrinterService.cancelJob", e);
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
					log.error("Exception on ZebraPrinterService.getStatus", e);
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

	public void print(String bmpBlackAsBase64, String bmpColorAsBase64, String bmpBackAsBase64, String bmpOverlayAsBase64) {
		try {
			List<GraphicsInfo> graphicsData = new ArrayList<GraphicsInfo>();
			graphicsData.add(drawImage(bmpColorAsBase64, PrintType.Color, CardSide.Front));
			graphicsData.add(drawImage(bmpBlackAsBase64, PrintType.MonoK, CardSide.Front));
			if(StringUtils.isNotEmpty(bmpBackAsBase64)) {
				graphicsData.add(drawImage(bmpBackAsBase64, PrintType.MonoK, CardSide.Back));
			}
			graphicsData.add(drawImage(bmpOverlayAsBase64, PrintType.Overlay, CardSide.Front));
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_SOURCE, CardSource.Feeder.name());
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_DESTINATION, CardDestination.Hold.name());
			jobId = zebraCardPrinter.print(1, graphicsData);
			pollJobStatus();
		} catch (Exception e) {
			throw new RuntimeException(e);
		}
	}

	private GraphicsInfo drawImage(String imageData, PrintType printType, CardSide side) throws IOException, ConnectionException, ZebraCardException {
		ZebraGraphics graphics = new ZebraCardGraphics(zebraCardPrinter);
		graphics.initialize(0, 0, OrientationType.Landscape, printType, Color.WHITE);
		graphics.drawImage(Base64.getDecoder().decode(imageData), 0, 0, 0, 0, printerZebraRotation ? RotationType.Rotate180FlipNone :  RotationType.RotateNoneFlipNone);
		ZebraCardImageI zebraCardImage = graphics.createImage();
		GraphicsInfo graphicsInfo = new GraphicsInfo();
		graphicsInfo.side = side;
		graphicsInfo.fillColor = -1;
		graphicsInfo.graphicData = zebraCardImage;
		graphicsInfo.graphicType = GraphicType.BMP;
		graphicsInfo.printType = printType;
		return graphicsInfo;
	}

	public void flipCard() {
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
	}
}
