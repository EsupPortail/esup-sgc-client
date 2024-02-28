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
import com.evolis.sdk.State;
import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.Resource;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.tasks.evolis.EvolisSdkPrintEncodeTask;
import org.esupportail.esupsgcclient.ui.EsupSgcDesfireFullTestPcscDialog;
import org.esupportail.esupsgcclient.ui.EsupSgcTestPcscDialog;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
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

	final static long DEFAULT_TIMEOUT = 3000;

	// 60 sec. : si une boite de dialogue evolis print center apparait lors de l'impression,
	// le temps de cliquer est comptabilisé dans l'impression ... et donc dans les 60 sec. de timeout
	// d'où le fait de ne pas mettre 'seulement' 30 sec ici
	// -> on conseille aux gestionnaires de désactiver les notifications dans evolis print center
	final static long DEFAULT_TIMEOUT_PRINT = 60000;

	ObjectMapper objectMapper = new ObjectMapper();

	@Resource
	AppConfig appConfig;

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

	@Override
	public String getMaintenanceInfo() {
		CleaningInfo cleaningInfo = getEvolisConnection().getCleaningInfo();
		String cleaningInfoString = String.format("Total Card Count : %s, CardCountBeforeWarrantyLost : %s, isPrintHeadUnderWarranty : %s, CardCountBeforeWarning : %s",
				cleaningInfo.getTotalCardCount(), cleaningInfo.getCardCountBeforeWarrantyLost(), cleaningInfo.isPrintHeadUnderWarranty(), cleaningInfo.getCardCountBeforeWarning());
		return cleaningInfoString;
	}

	private Connection getEvolisConnection() {
		if(evolisConnection == null || !evolisConnection.isOpen()) {
			init();
		}
		return evolisConnection;
	}

	@Override
	public void setupJfxUi(Stage stage, Tooltip tooltip, TextArea logTextarea, MenuBar menuBar) {

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
		stopEvolis.setText("Éteindre l'imprimante");
		Menu evolisMenu = new Menu();
		evolisMenu.setText("Evolis-SDK");
		evolisMenu.getItems().addAll(evolisRelease, evolisReset, evolisReject, evolisCommand, testPcsc, pcscDesfireTest, stopEvolis);
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

		stopEvolis.setOnAction(actionEvent -> {
			new Thread(() -> {
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis stop ... \n"));
				getEvolisConnection().sendCommand("Psdc;Force");
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis stopped OK \n"));
			}).start();
		});

		TilePane r = new TilePane();
		TextInputDialog td = new TextInputDialog("Echo;ESUP-SGC d'ESUP-Portail");
		td.setHeaderText("Lancer une commande à l'imprimante evolis");
		td.setContentText("Commande");
		evolisCommand.setOnAction(actionEvent -> {
			Optional<String> result = td.showAndWait();
			result.ifPresent(command -> {
				getEvolisConnection().sendCommand(command);
			});
		});

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
				}
			}
			if (evolisConnection == null) {
				logTextarea.appendText("No evolis printer found\n");
			}
		}
	}


	public String getPrinterStatus() {
		State state = getEvolisConnection().getState();
		return String.format("%s : %s", state.getMajorState(), state.getMinorState());
	}

	public void startSequence() {
		evolisProntSession = new PrintSession(evolisConnection);
		evolisProntSession.setAutoEject(false);
		getEvolisConnection().setInputTray(InputTray.FEEDER);
		getEvolisConnection().setOutputTray(OutputTray.STANDARD);
		getEvolisConnection().setErrorTray(OutputTray.ERROR);
	}


	public boolean printFrontColorBmp(String bmpColorAsBase64) {
		byte[] bytes = Base64.getDecoder().decode(bmpColorAsBase64);
		return evolisProntSession.setImage(CardFace.FRONT, bytes, bytes.length);
	}

	public boolean printFrontBlackBmp(String bmpBlackAsBase64) {
		byte[] bytes = Base64.getDecoder().decode(bmpBlackAsBase64);
		return evolisProntSession.setBlack(CardFace.FRONT, bytes, bytes.length);
	}

	public void print() {
		ReturnCode returnCode = evolisProntSession.print();
		logTextarea.appendText("Print return code : " + returnCode.name() + "\n");
	}

	public boolean insertCardToContactLessStation(EsupSgcTask esupSgcTask) {
		if(esupSgcTask.isCancelled()) {
			throw new RuntimeException("EvolisTask is cancelled");
		}
		return getEvolisConnection().setCardPos(CardPos.CONTACTLESS);
	}

	public void eject() {
		getEvolisConnection().ejectCard();
	}

	public void releaseIfNeeded() {
		getEvolisConnection().release();
	}

	public void reject() {
		getEvolisConnection().rejectCard();
	}
}

