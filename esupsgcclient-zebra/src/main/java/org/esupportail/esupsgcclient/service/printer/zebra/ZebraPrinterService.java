package org.esupportail.esupsgcclient.service.printer.zebra;

import java.util.Arrays;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import com.zebra.sdk.comm.UsbConnection;
import com.zebra.sdk.common.card.enumerations.CardDestination;
import com.zebra.sdk.common.card.enumerations.CardSource;
import com.zebra.sdk.common.card.enumerations.SmartCardEncoderType;
import com.zebra.sdk.common.card.enumerations.SmartCardType;
import com.zebra.sdk.common.card.printer.ZebraPrinterZxp;
import com.zebra.sdk.common.card.settings.ZebraCardSettingNames;
import com.zebra.sdk.device.ZebraIllegalArgumentException;
import com.zebra.sdk.printer.PrinterReconnectionHandler;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.ZebraPrinterLinkOs;
import com.zebra.sdk.util.internal.Sleeper;
import com.zebra.sdk.zxp.job.ZebraCardJobSettingNamesZxp;
import com.zebra.sdk.zxp.printer.internal.ZxpZebraPrinter;
import jakarta.annotation.Resource;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.TilePane;
import org.apache.log4j.Logger;

import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.ui.EsupSgcTestPcscDialog;
import org.esupportail.esupsgcclient.utils.Utils;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.comm.internal.CardError;
import com.zebra.sdk.common.card.containers.JobStatus;
import com.zebra.sdk.common.card.containers.JobStatusInfo;
import com.zebra.sdk.common.card.errors.ZebraCardErrors;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.jobSettings.ZebraCardJobSettingNames;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredUsbPrinter;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;
import com.zebra.sdk.settings.SettingsException;
import com.zebra.sdk.zxp.comm.internal.ZXPBase.Response;
import com.zebra.sdk.zxp.comm.internal.ZXPPrn;
import com.zebra.sdk.zxp.device.internal.ZxpDevice;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;

@Component
public class ZebraPrinterService extends EsupSgcPrinterService {
	
	private final static Logger log = Logger.getLogger(ZebraPrinterService.class);
	
	private static Integer CARD_FEED_TIMEOUT = 30000;

	@Resource
	ZebraHeartbeatTaskService zebraHeartbeatTaskService;

	@Resource
	EsupSgcTestPcscDialog esupSgcTestPcscDialog;

	Connection connection;
	ZebraCardPrinter zebraCardPrinter;
	ZXPPrn zxpPrn;
	int jobId;

	@Override
	public void setupJfxUi(Tooltip tooltip, TextArea logTextarea, MenuBar menuBar) {

		tooltip.textProperty().bind(zebraHeartbeatTaskService.titleProperty());
		zebraHeartbeatTaskService.start();
		zebraHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));

		MenuItem zebraReject = new MenuItem();
		zebraReject.setText("Rejeter la carte");
		MenuItem zebraPrintEnd = new MenuItem();
		zebraPrintEnd.setText("Clore la session d'impression");
		MenuItem testPcsc = new MenuItem();
		testPcsc.setText("Stress test pc/sc");
		MenuItem zebraCommand = new MenuItem();
		zebraCommand.setText("Envoyer une commande avancée à l'imprimante");
		Menu zebraMenu = new Menu();
		zebraMenu.setText("Zebra");
		zebraMenu.getItems().addAll(zebraReject, zebraPrintEnd, testPcsc, zebraCommand);
		menuBar.getMenus().add(zebraMenu);

		zebraReject.setOnAction(actionEvent -> {
			new Thread(() -> {eject();});
		});

		testPcsc.setOnAction(actionEvent -> {
			esupSgcTestPcscDialog.getTestPcscDialog(
					()-> {
						launchEncoding();
						},
					()->{
						try {
							zebraCardPrinter.resume();
						} catch (ConnectionException|ZebraCardException e) {
							log.warn(e);
						}
						eject();
					}
			).show();
		});

		TilePane r = new TilePane();
		TextInputDialog td = new TextInputDialog("!CC");
		td.setHeaderText("Lancer une commande à l'imprimante zebra");
		td.setContentText("Commande");
		zebraCommand.setOnAction(actionEvent -> {
			Optional<String> result = td.showAndWait();
			result.ifPresent(command -> {
				new Thread(() -> {
					logTextarea.appendText("Send command to zebra : " + command + "\n");
					//connection.write(HexFormat.of().parseHex("1b" + "4d49" + "0d"));
					//connection.write(HexFormat.of().parseHex("1b" + "4d53" + "0d"));
					// prefix with ESC and suffix with ReturnCarriage
					String cmd = (char)27 + command + (char)13;

					String resp = new String();
					try {
						if (!connection.isConnected()) {
							log.info("zebra not connected - try to connect");
							connection.open();
						}
						connection.sendAndWaitForResponse(cmd.getBytes(), 2000, 2000, resp);
						logTextarea.appendText("Response from zebra : " + resp + "\n");
					} catch (ConnectionException ex) {
						log.warn(String.format("zebra exception sending command %s : %s", command, ex.getMessage()), ex);
						logTextarea.appendText("zebra exception : " + ex.getMessage() + "\n");
					}
				}).start();
			});
		});

	}
	
	public void init() throws ConnectionException{

		DiscoveredUsbPrinter[] discoveredPrinters;
		discoveredPrinters = UsbDiscoverer.getZebraUsbPrinters();
		for(DiscoveredUsbPrinter discoveredUsbPrinter : discoveredPrinters) {
			DiscoveredPrinter discoveredPrinter =  discoveredUsbPrinter;
			connection = discoveredPrinter.getConnection();
			try {
				if (!connection.isConnected()) {
					log.info("zebra not connected - try to connect");
					connection.open();
				}
				zebraCardPrinter = ZebraCardPrinterFactory.getZxpPrinter(connection);
				ZxpDevice zxpDevice = new ZxpDevice(connection);
                zxpPrn = zxpDevice.getZxpPrinter();
				break;
			} catch(Exception e) {
				log.error("Zebra init error", e);
				throw new ConnectionException(e);
			}
		}

		//connection.write(HexFormat.of().parseHex("1b" + "4d49" + "0d"));
		//connection.write(HexFormat.of().parseHex("1b" + "4d53" + "0d"));
		//String cmd = "MM " + Integer.toString(4000) + " " + Integer.toString(1);
		//connection.write(HexFormat.of().parseHex("1b" + HexFormat.of().formatHex(cmd.getBytes()) + "0d"));

		try {
			log.info("Settings range of offset : " + zebraCardPrinter.getSettingRange(ZebraCardSettingNames.SMARTCARD_X_OFFSET));
			log.info("Settings range of internal encoder contactless : " + zebraCardPrinter.getSettingRange(ZebraCardSettingNames.INTERNAL_ENCODER_CONTACTLESS_ENCODER));
			// zebraCardPrinter.setSetting(ZebraCardSettingNames.SMARTCARD_X_OFFSET, "350");
			log.info(String.format("Printer Firmware : %s", zebraCardPrinter.getPrinterInformation().firmwareVersion));
			log.info(String.format("Printer cards count : %s", zebraCardPrinter.getCardCount().totalCards));
			log.info(String.format("Printer sensor states : %s", zebraCardPrinter.getSensorStates()));
		} catch (Exception e) {
			log.warn(e);
		}
	}
	public void setSmartcardJob(){
		try {
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_SOURCE, CardSource.Feeder.name());
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_DESTINATION, CardDestination.Hold.name());
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS, SmartCardEncoderType.MIFARE.name());
		} catch (SettingsException e) {
			log.error("Zebra settings error", e);
		}
	}

	public void launchEncoding() {
		try {
			setSmartcardJob();
			zebraCardPrinter.positionCard();
		} catch (Exception e) {
			log.error("Zebra card encoder activation error", e);
		}
	}
	
	public boolean pollJobStatus(){
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
	
	public void resume() throws ZebraCardException{
		try {
			zebraCardPrinter.resume();
		} catch (ZebraCardException | ConnectionException e) {
			log.error("Zebra resume error", e);
			throw new ZebraCardException("zebraCardPrinter is null - try reset printer");
		}
	}

	public void reverseCard() throws ZebraCardException{
		log.info("try to reverse card");
		try {
			zxpPrn.flipCard(new Response(), new CardError());
		} catch (ConnectionException e) {
			log.error("Zebra resume error", e);
			throw new ZebraCardException("zebraCardPrinter is null - try reset printer");
		}
	}
	
	public void reset() throws ZebraCardException {
		try {
			zebraCardPrinter.reset();
		} catch (ZebraCardException | ConnectionException e) {
			log.error("Zebra resume error", e);
			throw new ZebraCardException("zebraCardPrinter is null - try reset printer");
		}
	}
	
	public String getStatus() {
		String status = null;
		if(zebraCardPrinter != null) {
			while(status == null){
				Utils.sleep(250);
				try {
					log.trace(zebraCardPrinter.getPrinterStatus().status +" : " +  zebraCardPrinter.getPrinterStatus().alarmInfo.description);
					status = zebraCardPrinter.getPrinterStatus().status + " " + zebraCardPrinter.getPrinterStatus().alarmInfo.description;
				} catch (ConnectionException | SettingsException | ZebraCardException e) {
					log.error(e);
				}
			}
		}
		return status;
	}

	public int getJobId() {
		return jobId;
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
	public String getMaintenanceInfo() {
		return getStatus();
	}



	public void eject() {
		try {
			zebraCardPrinter.ejectCard();
		} catch (ConnectionException e) {
			throw new RuntimeException(e);
		} catch (ZebraCardException e) {
			throw new RuntimeException(e);
		}
	}
}
