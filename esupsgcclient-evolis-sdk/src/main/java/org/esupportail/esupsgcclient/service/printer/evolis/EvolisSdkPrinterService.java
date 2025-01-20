package org.esupportail.esupsgcclient.service.printer.evolis;

import com.evolis.sdk.*;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.EsupSgcDesfireFullTestPcscDialog;
import org.esupportail.esupsgcclient.ui.EsupSgcTestPcscDialog;
import org.esupportail.esupsgcclient.ui.FileLocalStorage;
import org.esupportail.esupsgcclient.ui.LogTextAreaService;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.*;


@Component
public class EvolisSdkPrinterService extends EsupSgcPrinterService {
	
	final static Logger log = LoggerFactory.getLogger(EvolisSdkPrinterService.class);

	@Resource
	EvolisSdkHeartbeatTaskService evolisSdkHeartbeatTaskService;

	@Resource
	EsupSgcTestPcscDialog esupSgcTestPcscDialog;

	@Resource
	EsupSgcDesfireFullTestPcscDialog esupSgcDesfireFullTestPcscDialog;

	@Resource
	AppSession appSession;

	@Resource
	FileLocalStorage fileLocalStorage;

	@Resource
	LogTextAreaService logTextAreaService;

	Connection evolisConnection;

	PrintSession evolisProntSession;

	String ribbonInfoString4MaintenanceInfo = "";

	Date lastRibbonInfoDate = new Date();


	@Override
	public synchronized String getMaintenanceInfo() {
		CleaningInfo cleaningInfo = getEvolisConnection().getCleaningInfo();
		// check ribbon only if printer is ready and only each hour to avoid too much requests
		if(ribbonInfoString4MaintenanceInfo.isEmpty() || (lastRibbonInfoDate.getTime() + 1000*3600) < new Date().getTime()) {
			ribbonInfoString4MaintenanceInfo = getRibbonInfoString();
			lastRibbonInfoDate = new Date();
		}
		String printerInfoString = String.format("%s\n%s\nTotal Card Count : %s, CardCountBeforeWarrantyLost : %s, isPrintHeadUnderWarranty : %s, CardCountBeforeWarning : %s\n",
				getInfo(), ribbonInfoString4MaintenanceInfo,
				cleaningInfo.getTotalCardCount(), cleaningInfo.getCardCountBeforeWarrantyLost(), cleaningInfo.isPrintHeadUnderWarranty(), cleaningInfo.getCardCountBeforeWarning());
		return printerInfoString;
	}

	protected Connection getEvolisConnection() {
		if(evolisConnection == null || !evolisConnection.isOpen()) {
			init();
		}
		return evolisConnection;
	}

	@Override
	public synchronized void setupJfxUi(Stage stage, Tooltip tooltip, MenuBar menuBar) {

		init();

		tooltip.textProperty().bind(evolisSdkHeartbeatTaskService.titleProperty());
		evolisSdkHeartbeatTaskService.start();
		evolisSdkHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextAreaService.appendText(newValue));

		MenuItem evolisRelease = new MenuItem();
		evolisRelease.setText("Release Evolis");
		MenuItem evolisReset = new MenuItem();
		evolisReset.setText("Reset Evolis");
		MenuItem evolisReject = new MenuItem();
		evolisReject.setText("Rejeter la carte");
		MenuItem evolisCommand = new MenuItem();
		evolisCommand.setText("Envoyer une commande avancée à l'imprimante");
		MenuItem testPcsc = new MenuItem();
		testPcsc.setText("Stress test pc/sc");
		MenuItem pcscDesfireTest = new MenuItem();
		pcscDesfireTest.setText("Stress test PC/SC DES Blank Desfire");
		MenuItem stopEvolis = new MenuItem();
		MenuItem clearPrintStatusMenu = new MenuItem();
		clearPrintStatusMenu.setText("Clear Printer status");
		stopEvolis.setText("Éteindre l'imprimante");
		MenuItem stopEpcSupervision = new MenuItem();
		stopEpcSupervision.setText("Arrêter la supervision EPC de l'imprimante");
		MenuItem restartEpcSupervision = new MenuItem();
		restartEpcSupervision.setText("Redémarrer la supervision EPC de l'imprimante");
		Menu evolisMenu = new Menu();
		evolisMenu.setText("Evolis-SDK");
		evolisMenu.getItems().addAll(evolisRelease, evolisReset, evolisReject,
				evolisCommand, testPcsc, pcscDesfireTest, stopEvolis, clearPrintStatusMenu,
				stopEpcSupervision, restartEpcSupervision);
		menuBar.getMenus().add(evolisMenu);

		evolisRelease.setOnAction(actionEvent -> {
			new Thread(() -> {
				logTextAreaService.appendText("Evolis release ...");
				getEvolisConnection().release();
				logTextAreaService.appendText("Evolis release OK");
			}).start();
		});

		evolisReset.setOnAction(actionEvent -> {
			new Thread(() -> {
				logTextAreaService.appendText("Evolis reset ...");
				getEvolisConnection().reset();
				logTextAreaService.appendText("Evolis reset OK");
			}).start();
		});

		evolisReject.setOnAction(actionEvent -> {
			new Thread(() -> {
				logTextAreaService.appendText("Evolis reject ...");
				getEvolisConnection().rejectCard();
				logTextAreaService.appendText("Evolis reject OK");
			}).start();
		});


		testPcsc.setOnAction(actionEvent -> {
			esupSgcTestPcscDialog.getTestPcscDialog(
					()->getEvolisConnection().setCardPos(CardPos.CONTACTLESS),
					()->getEvolisConnection().ejectCard()
			).show();
		});
		testPcsc.disableProperty().bind(appSession.nfcReadyProperty().not().or(appSession.taskIsRunningProperty()).or(appSession.printerReadyProperty().not()));

		pcscDesfireTest.setOnAction(actionEvent -> {
			esupSgcDesfireFullTestPcscDialog.getTestPcscDialog(
					()->getEvolisConnection().setCardPos(CardPos.CONTACTLESS),
					()->getEvolisConnection().ejectCard()
			).show();
		});
		pcscDesfireTest.disableProperty().bind(appSession.nfcReadyProperty().not().or(appSession.taskIsRunningProperty()).or(appSession.printerReadyProperty().not()));

		clearPrintStatusMenu.setOnAction(actionEvent -> {
			new Thread(() -> {
				clearPrintStatus();
			}).start();
		});

		stopEvolis.setOnAction(actionEvent -> {
			new Thread(() -> {
				logTextAreaService.appendText("Evolis stop ...");
				getEvolisConnection().sendCommand("Psdc;Force");
				logTextAreaService.appendText("Evolis stopped OK");
			}).start();
		});

		stopEpcSupervision.setOnAction(actionEvent -> {
			new Thread(() -> {
				logTextAreaService.appendText("Arrêt de la supervision ...");
				stopEpcSupervision();
				logTextAreaService.appendText("supervision arrêtée :");
			}).start();
		});

		restartEpcSupervision.setOnAction(actionEvent -> {
			new Thread(() -> {
				logTextAreaService.appendText("Redémarrage de la supervision ...");
				stopEpcSupervision();
				logTextAreaService.appendText("supervision démarrée :");
			}).start();
		});

		TilePane r = new TilePane();
		TextInputDialog td = new TextInputDialog("Echo;ESUP-SGC d'ESUP-Portail");
		td.setHeaderText("Lancer une commande à l'imprimante evolis");
		td.setContentText("Commande");
		evolisCommand.setOnAction(actionEvent -> {
			Optional<String> result = td.showAndWait();
			result.ifPresent(command -> {
				new Thread(() -> {
					logTextAreaService.appendText("Call : " + command + " ...");
					String ret = getEvolisConnection().sendCommand(command);
					logTextAreaService.appendText("Return : " + ret);
				}).start();
			});
		});

	}

	public synchronized void insertCardPrinter() {
		getEvolisConnection().sendCommand("Si;");
	}

	public synchronized void init() {
		if(evolisConnection == null || !getEvolisConnection().isOpen()) {
			log.debug("Evolis init connection ...");
			Device[] devicesArray = Evolis.getDevices();
			if (devicesArray != null) {
				List<Device> devices = Arrays.asList(devicesArray);
				if (devices.isEmpty()) {
					logTextAreaService.appendTextOnlyOne("No evolis printer found");
				} else {
					Device device = devices.get(0);
					logTextAreaService.appendText("Evolis printer found : " + device.getName());
					evolisConnection = new Connection(device);
				}
			}
			if (evolisConnection == null) {
				logTextAreaService.appendTextOnlyOne("No evolis printer found");
			} else {
				String progressDescRibbonInfo = getRibbonInfoString();
				if(!progressDescRibbonInfo.isEmpty()) {
					logTextAreaService.appendText(progressDescRibbonInfo);
				}
				String printerInfoString = getInfo();
				if(!printerInfoString.isEmpty()) {
					logTextAreaService.appendText(printerInfoString);
				}
			}
		}
	}

	public synchronized String getInfo() {
		String printerInfoString = "";
		PrinterInfo printerInfo = getEvolisConnection().getInfo();
		if(printerInfo != null) {
			printerInfoString = String.format("%s / %s - sn %s - firmware %s",
					printerInfo.getMarkName(),
					printerInfo.getModelName(),
					printerInfo.getSerialNumber(),
					printerInfo.getFwVersion());
		}
		return printerInfoString;
	}

	public synchronized RibbonInfo getRibbonInfo() {
		return getEvolisConnection().getRibbonInfo();
	}

	public String getRibbonInfoString() {
		String progressDesc = "";
		RibbonInfo ribbonInfo = getRibbonInfo();
		if(ribbonInfo != null) {
			progressDesc = String.format("Ribbon Info - %s : reste %s / %s faces", ribbonInfo.getDescription(), ribbonInfo.getRemaining(), ribbonInfo.getCapacity());
		}
		return progressDesc;
	}

	public String getPrinterStatus() {
		State state = getEvolisConnection().getState();
		String printerStatus = String.format("%s : %s", state.getMajorState(), state.getMinorState());
		if(printerStatus.contains("WARNING : DEF_RIBBON_ENDED")) {
			// Hack - WARNING : DEF_RIBBON_ENDED can be occurred on Evolis Primacy2
			// even if the ribbon is not ended -> we clear the status to avoid blocking the printer
			// qd on encode puis imprime, le clear print status est problématique : si on fait un clearstatus après encodage, la carte est rejetée et une autre carte est ensuite imprimée
			RibbonInfo ribbonInfo = getRibbonInfo();
			if(ribbonInfo.getRemaining()>0) {
				logTextAreaService.appendText("Hack - clear printer status : " + printerStatus + " - ribbon remaining : " + ribbonInfo.getRemaining() + " but status is WARNING : DEF_RIBBON_ENDED");
				clearPrintStatus();
				state = getEvolisConnection().getState();
				printerStatus = String.format("%s : %s", state.getMajorState(), state.getMinorState());
				logTextAreaService.appendText("Hack - new printer status : " + printerStatus);
			}
		}
		return printerStatus;
	}

	protected synchronized PrintSession getPrintSession() {
		if(evolisProntSession == null || getPrinterStatus().contains("PRINTER_OFFLINE")) {
			evolisProntSession = new PrintSession(evolisConnection);
			logTextAreaService.appendText("New PrintSession OK");
		}
		return evolisProntSession;
	}

	public synchronized void newPrintSessionWithNoAutoEject() {
		getPrintSession().setAutoEject(false);
	}

	public synchronized void setupTrayConnection() {
		getEvolisConnection().setInputTray(InputTray.FEEDER);
		getEvolisConnection().setOutputTray(OutputTray.STANDARD);
		getEvolisConnection().setErrorTray(OutputTray.ERROR);
	}

	public synchronized boolean printFrontColorBmp(String bmpColorAsBase64) {
		byte[] bytes = Base64.getDecoder().decode(bmpColorAsBase64);
		return getPrintSession().setImage(CardFace.FRONT, bytes);
	}

	public synchronized boolean printFrontBlackBmp(String bmpBlackAsBase64) {
		byte[] bytes = Base64.getDecoder().decode(bmpBlackAsBase64);
		return getPrintSession().setBlack(CardFace.FRONT, bytes);
	}

	public synchronized boolean printBackBmp(String bmpBackAsBase64) {
		byte[] bytes = Base64.getDecoder().decode(bmpBackAsBase64);
		return getPrintSession().setBlack(CardFace.BACK, bytes);
	}

	public synchronized void print() {
		ReturnCode returnCode = getPrintSession().print();
		logTextAreaService.appendText("Print return code : " + returnCode.name());
	}

	public synchronized boolean insertCardToContactLessStation(EsupSgcTask esupSgcTask) {
		if(esupSgcTask.isCancelled()) {
			throw new RuntimeException("EvolisTask is cancelled");
		}
		return getEvolisConnection().setCardPos(CardPos.CONTACTLESS);
	}

	public synchronized void eject() {
		getEvolisConnection().ejectCard();
	}

	public synchronized void releaseIfNeeded() {
		getEvolisConnection().release();
	}

	public synchronized void reject() {
		getEvolisConnection().rejectCard();
	}

	public synchronized void stopEpcSupervision() {
		if(Service.isRunning()) {
			Service.stop();
		}
	}

	public synchronized void restartEpcSupervision() {
		if(Service.isRunning()) {
			Service.restart();
		} else {
			Service.start();
		}
	}

	public synchronized void clearPrintStatus() {
		logTextAreaService.appendText("Clear Printer status ...");
		getEvolisConnection().sendCommand("Scs;");
	}

	public synchronized void closeConnection() {
		if(evolisConnection != null && evolisConnection.isOpen()) {
			evolisConnection.close();
			logTextAreaService.appendText("Evolis connection closed");
			init();
		}
	}
}

