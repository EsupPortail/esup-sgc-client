package org.esupportail.esupsgcclient;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisHeartbeatTaskService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.webcam.EsupWebcamDiscoveryListener;
import org.esupportail.esupsgcclient.service.webcam.WebcamTaskService;

import com.github.sarxos.webcam.Webcam;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.ui.FileLocalStorage;
import org.esupportail.esupsgcclient.utils.Utils;

public class EsupSgcClientJfxController {

	final static Logger log = Logger.getLogger(EsupSgcClientJfxController.class);

	public enum StyleLevel {success, danger, warning, primary, info};

	public Stage primaryStage;

	@FXML
	private FlowPane actionsPane;

	@FXML
	private MenuItem buttonLogs;

	@FXML
	private MenuItem buttonNfcTag;

	@FXML
	private MenuItem evolisReject;

	@FXML
	private MenuItem evolisPrintEnd;

	@FXML
	private Button checkAuth;

	@FXML
	private Button checkCamera;

	@FXML
	private Button checkNfc;

	@FXML
	private Button checkPrinter;

	@FXML
	private ComboBox<String> comboBox;

	@FXML
	public TextArea logTextarea;

	@FXML
	public Pane nfcTagPane;

	@FXML
	private FlowPane panePrincipal;

	@FXML
	private Label textPrincipal;

	@FXML
	public ImageView webcamImageView;

	@FXML
	public ImageView bmpBlackImageView;

	@FXML
	public ImageView bmpColorImageView;

	@FXML
	private ProgressBar progressBar;

	public static SimpleBooleanProperty webcamReady = new SimpleBooleanProperty(false);

	public SimpleBooleanProperty nfcReady = new SimpleBooleanProperty();

	public static SimpleBooleanProperty authReady = new SimpleBooleanProperty();

	WebcamTaskService webcamTaskService;

	EsupSgcTaskServiceFactory esupSgcTaskServiceFactory;

	public void init(String esupNfcTagServerUrl) {

		esupSgcTaskServiceFactory = new EsupSgcTaskServiceFactory(webcamImageView, bmpColorImageView, bmpBlackImageView, logTextarea, progressBar, textPrincipal, actionsPane);

		nfcTagPane.getChildren().add(new EsupNfcClientStackPane(esupNfcTagServerUrl, Utils.getMacAddress()));
		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldWebcamName, newWebcamName) -> {
			log.debug("comboBox SelectionModel Event : " + options.getValue() + " - " +  oldWebcamName + " - " + newWebcamName);
			if(options.getValue()!=null && newWebcamName!=null && !newWebcamName.equals(oldWebcamName)) {
				if(webcamTaskService != null && webcamTaskService.isRunning()) {
					webcamTaskService.cancel();
				}
				webcamTaskService = new WebcamTaskService(newWebcamName, webcamImageView);
				webcamTaskService.start();
				checkCamera.getTooltip().setText(newWebcamName);
			}
		});

		buttonLogs.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				if (logTextarea.isVisible()) {
					logTextarea.setVisible(false);
					logTextarea.setManaged(false);
					buttonLogs.setText("Afficher les logs");
				} else {
					logTextarea.setVisible(true);
					logTextarea.setManaged(true);
					buttonLogs.setText("Masquer les logs");
				}
				primaryStage.sizeToScene();
			}
		});

		buttonNfcTag.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				if (nfcTagPane.isVisible()) {
					nfcTagPane.setVisible(false);
					nfcTagPane.setManaged(false);
					buttonNfcTag.setText("Afficher EsupNfcTag");
				} else {
					nfcTagPane.setVisible(true);
					nfcTagPane.setManaged(true);
					buttonNfcTag.setText("Masquer EsupNfcTag");
				}
				primaryStage.sizeToScene();
			}
		});


		nfcReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkNfc.getStyleClass().clear();
					checkNfc.getStyleClass().add("btn-success");
					logTextarea.appendText("PC/SC OK\n");
					startLoopServiceIfPossible();
				} else {
					checkNfc.getStyleClass().clear();
					checkNfc.getStyleClass().add("btn-danger");
				}
			}
		});

		authReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkAuth.getStyleClass().clear();
					checkAuth.getStyleClass().add("btn-success");
					checkAuth.getTooltip().setText(FileLocalStorage.eppnInit);
					logTextarea.appendText("Authentification OK : " + FileLocalStorage.eppnInit + "\n");
					startLoopServiceIfPossible();
				} else {
					checkAuth.getStyleClass().clear();
					checkAuth.getStyleClass().add("btn-danger");
					checkAuth.getTooltip().setText("...");
				}
			}
		});
		evolisReject.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				Thread th = new Thread(new Task<>() {
					@Override
					protected Object call() throws Exception {
						EvolisPrinterService.reject();
						return null;
					}
				});
				th.setDaemon(true);
				th.start();
			}
		});
		evolisPrintEnd.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				Thread th = new Thread(new Task<>() {
					@Override
					protected Object call() throws Exception {
						EvolisPrinterService.printEnd();
						return null;
					}
				});
				th.setDaemon(true);
				th.start();
			}
		});

		EvolisHeartbeatTaskService.printerReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkPrinter.getStyleClass().clear();
					checkPrinter.getStyleClass().add("btn-success");
					logTextarea.appendText("imprimante evolis OK\n");
					bmpBlackImageView.setVisible(true);
					bmpBlackImageView.setManaged(true);
					bmpColorImageView.setVisible(true);
					bmpColorImageView.setManaged(true);
					// webcamImageView.setVisible(false);
					// webcamImageView.setManaged(false);
					// primaryStage.sizeToScene();
					startLoopServiceIfPossible();
				} else {
					checkPrinter.getStyleClass().clear();
					checkPrinter.getStyleClass().add("btn-danger");
				}
			}
		});


		webcamReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkCamera.getStyleClass().clear();
					checkCamera.getStyleClass().add("btn-success");
					webcamImageView.setVisible(true);
					webcamImageView.setManaged(true);
					startLoopServiceIfPossible();
				} else {
					checkCamera.getStyleClass().clear();
					checkCamera.getStyleClass().add("btn-danger");
					logTextarea.appendText("Caméra déconnectée ?!\n");
				}
			}
		});

		EvolisHeartbeatTaskService evolisHeartbeatTaskService = new EvolisHeartbeatTaskService();
		checkPrinter.getTooltip().textProperty().bind(evolisHeartbeatTaskService.titleProperty());
		evolisHeartbeatTaskService.start();

		Webcam.addDiscoveryListener(new EsupWebcamDiscoveryListener(this));
		Webcam.getWebcams(); // with this webcams are discovered and listener works at startup

		/* just for testing ...
		WaitTaskService waitTaskService = new WaitTaskService();
		setupFlowEsupSgcTaskService(waitTaskService, null);
		*/

	}

	public synchronized void addWebcamComboBox(String webcamName) {
		if(!comboBox.getItems().contains(webcamName)) {
			comboBox.getItems().add(webcamName);
		}
		if(comboBox.getSelectionModel().getSelectedItem() == null && comboBox.getItems().size()>0) {
			comboBox.getSelectionModel().select(0);
		}
	}
	public synchronized void removeWebcamComboBox(String webcamName) {
		comboBox.getItems().remove(webcamName);
	}

	private void startLoopServiceIfPossible() {
		log.debug("startLoopServiceIfPossible ...");
		if(authReady.getValue() && nfcReady.getValue() && webcamReady.getValue()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					esupSgcTaskServiceFactory.runQrCodeTaskService();
					logTextarea.appendText("qrCodeTaskService is now running\n");
				}
			});
		}
		if(authReady.getValue() && nfcReady.getValue() && EvolisHeartbeatTaskService.printerReady.getValue()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					esupSgcTaskServiceFactory.runEvolisEsupSgcLongPollTaskService();
					logTextarea.appendText("evolisEsupSgcLongPollTaskService is now running\n");
				}
			});
		}
	}

	public void changeTextPrincipal(String text, StyleLevel styleLevel) {
		textPrincipal.setText(text);
		panePrincipal.getStyleClass().clear();
		panePrincipal.getStyleClass().add("panel-" + styleLevel);
	}

}
