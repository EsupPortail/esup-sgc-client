package org.esupportail.esupsgcclient.service.printer.evolis;

import com.evolis.sdk.CardFace;
import com.evolis.sdk.CardPos;
import com.evolis.sdk.CleaningInfo;
import com.evolis.sdk.Connection;
import com.evolis.sdk.Device;
import com.evolis.sdk.Evolis;
import com.evolis.sdk.InputTray;
import com.evolis.sdk.OutputTray;
import com.evolis.sdk.PrintSession;
import com.evolis.sdk.ReturnCode;
import com.evolis.sdk.RibbonInfo;
import com.evolis.sdk.Service;
import com.evolis.sdk.State;
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
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.util.Arrays;
import java.util.Base64;
import java.util.List;
import java.util.Optional;


/**
 * This Service provides commands for interaction with sgc from evolis printers
 * It computes JSON-RPC commands to send to Evolis Services Provider 2
 */
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

	Connection evolisConnection;

	PrintSession evolisProntSession;

	TextArea logTextarea;

	CheckMenuItem simulateMenuItem;

	@Override
	public synchronized String getMaintenanceInfo() {
		CleaningInfo cleaningInfo = getEvolisConnection().getCleaningInfo();
		String cleaningInfoString = String.format("Total Card Count : %s, CardCountBeforeWarrantyLost : %s, isPrintHeadUnderWarranty : %s, CardCountBeforeWarning : %s",
				cleaningInfo.getTotalCardCount(), cleaningInfo.getCardCountBeforeWarrantyLost(), cleaningInfo.isPrintHeadUnderWarranty(), cleaningInfo.getCardCountBeforeWarning());
		return cleaningInfoString;
	}

	protected Connection getEvolisConnection() {
		if(evolisConnection == null || !evolisConnection.isOpen()) {
			init();
		}
		return evolisConnection;
	}

	@Override
	public synchronized void setupJfxUi(Stage stage, Tooltip tooltip, TextArea logTextarea, MenuBar menuBar) {

		this.logTextarea = logTextarea;
		init();

		tooltip.textProperty().bind(evolisSdkHeartbeatTaskService.titleProperty());
		evolisSdkHeartbeatTaskService.start();
		evolisSdkHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(newValue + "\n")));

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
		simulateMenuItem = new CheckMenuItem();
		simulateMenuItem.setText("Simuler l'impression");
		Menu evolisMenu = new Menu();
		evolisMenu.setText("Evolis-SDK");
		evolisMenu.getItems().addAll(evolisRelease, evolisReset, evolisReject,
				evolisCommand, testPcsc, pcscDesfireTest, stopEvolis, clearPrintStatusMenu,
				stopEpcSupervision, restartEpcSupervision, simulateMenuItem);
		menuBar.getMenus().add(evolisMenu);

		evolisRelease.setOnAction(actionEvent -> {
			new Thread(() -> {
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis release ... \n"));
				getEvolisConnection().release();
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis release OK\n"));
			}).start();
		});

		evolisReset.setOnAction(actionEvent -> {
			new Thread(() -> {
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis reset ... \n"));
				getEvolisConnection().reset();
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis reset OK\n"));
			}).start();
		});

		evolisReject.setOnAction(actionEvent -> {
			new Thread(() -> {
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis reject ... \n"));
				getEvolisConnection().rejectCard();
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis reject OK \n"));
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
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis stop ... \n"));
				getEvolisConnection().sendCommand("Psdc;Force");
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis stopped OK \n"));
			}).start();
		});

		stopEpcSupervision.setOnAction(actionEvent -> {
			new Thread(() -> {
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Arrêt de la supervision ... \n"));
				stopEpcSupervision();
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("supervision arrêtée : \n"));
			}).start();
		});

		restartEpcSupervision.setOnAction(actionEvent -> {
			new Thread(() -> {
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Redémarrage de la supervision ... \n"));
				stopEpcSupervision();
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("supervision démarrée : \n"));
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
					Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Call : " + command + " ... \n"));
					String ret = getEvolisConnection().sendCommand(command);
					Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Return : " + ret + "\n"));
				}).start();
			});
		});

	}

	public synchronized void insertCardPrinter() {
		getEvolisConnection().sendCommand("Si;");
	}

	public synchronized void init() {
		if(evolisConnection == null || !getEvolisConnection().isOpen()) {
			log.info("Evolis init connection ...");
			Device[] devicesArray = Evolis.getDevices();
			if (devicesArray != null) {
				List<Device> devices = Arrays.asList(devicesArray);
				if (devices.isEmpty()) {
					logTextarea.appendText("No evolis printer found\n");
				} else {
					Device device = devices.get(0);
					logTextarea.appendText("Evolis printer found : " + device.getName() + "\n");
					evolisConnection = new Connection(device);
					RibbonInfo ribbonInfo = getEvolisConnection().getRibbonInfo();
					String progressDesc = String.format ("Ribbon Info - %s : reste %s / %s faces\n", ribbonInfo.getDescription(), ribbonInfo.getRemaining(), ribbonInfo.getCapacity());
					logTextarea.appendText(progressDesc);
				}
			}
			if (evolisConnection == null) {
				logTextarea.appendText("No evolis printer found\n");
			}
		}
	}


	public synchronized String getPrinterStatus() {
		State state = getEvolisConnection().getState();
		String printerStatus = String.format("%s : %s", state.getMajorState(), state.getMinorState());
		if(printerStatus.contains("WARNING : DEF_RIBBON_ENDED")) {
			// Hack - WARNING : DEF_RIBBON_ENDED can be occurred on Evolis Primacy2
			// even if the ribbon is not ended -> we clear the status to avoid blocking the printer
			clearPrintStatus();
			state = getEvolisConnection().getState();
			printerStatus = String.format("%s : %s", state.getMajorState(), state.getMinorState());
		}
		return printerStatus;
	}

	protected PrintSession getPrintSession() {
		if(evolisProntSession == null || getPrinterStatus().contains("PRINTER_OFFLINE")) {
			evolisProntSession = new PrintSession(evolisConnection);
			getEvolisConnection().setInputTray(InputTray.FEEDER);
			getEvolisConnection().setOutputTray(OutputTray.STANDARD);
			getEvolisConnection().setErrorTray(OutputTray.ERROR);
			logTextarea.appendText("PrintSession OK\n");
		}
		return evolisProntSession;
	}

	public synchronized void startSequence() {
		getPrintSession().setAutoEject(false);
	}

	public synchronized boolean printFrontColorBmp(String bmpColorAsBase64) {
		byte[] bytes = Base64.getDecoder().decode(bmpColorAsBase64);
		return getPrintSession().setImage(CardFace.FRONT, bytes, bytes.length);
	}

	public synchronized boolean printFrontBlackBmp(String bmpBlackAsBase64) {
		byte[] bytes = Base64.getDecoder().decode(bmpBlackAsBase64);
		return getPrintSession().setBlack(CardFace.FRONT, bytes, bytes.length);
	}

	public synchronized boolean printBackBmp(String bmpBackAsBase64) {
		byte[] bytes = Base64.getDecoder().decode(bmpBackAsBase64);
		return getPrintSession().setBlack(CardFace.BACK, bytes, bytes.length);
	}

	public synchronized void print() {
		ReturnCode returnCode = getPrintSession().print();
		logTextarea.appendText("Print return code : " + returnCode.name() + "\n");
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

	public boolean isSimulate() {
		return simulateMenuItem.isSelected();
	}

	public synchronized void clearPrintStatus() {
		Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Clear Printer status ... \n"));
		getEvolisConnection().sendCommand("Scs;");
	}
}

