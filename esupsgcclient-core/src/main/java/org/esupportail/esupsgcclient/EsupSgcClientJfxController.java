package org.esupportail.esupsgcclient;

import com.github.eduramiba.webcamcapture.drivers.NativeDriver;
import com.github.sarxos.webcam.Webcam;
import javax.annotation.Resource;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.Button;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Pane;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.NfcHeartbeatTaskService;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.service.webcam.EsupWebcamDiscoveryListener;
import org.esupportail.esupsgcclient.service.webcam.WebcamTaskService;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.ui.EsupSgcDesfireFullTestPcscDialog;
import org.esupportail.esupsgcclient.ui.EsupSgcTestPcscDialog;
import org.esupportail.esupsgcclient.ui.FileLocalStorage;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URL;
import java.util.ResourceBundle;

@Component
public class EsupSgcClientJfxController implements Initializable {

	final static Logger log = Logger.getLogger(EsupSgcClientJfxController.class);

	@Resource
	AppSession appSession;

	@Autowired(required = false)
	EsupSgcPrinterService esupSgcPrinterService;

	@Resource
	NfcHeartbeatTaskService nfcHeartbeatTaskService;

	@Resource
	FileLocalStorage fileLocalStorage;

	@FXML
	CheckMenuItem autostart;

	@FXML
	MenuItem reinitAndExit;

	@FXML
	MenuItem pcscTest;

	@FXML
	MenuItem pcscDesfireTest;

	@FXML
	MenuItem exit;

	@FXML
	MenuBar menuBar;

	@FXML
	FlowPane actionsPane;

	@FXML
	CheckMenuItem buttonDisplayStatut;

	@FXML
	CheckMenuItem buttonDisplayEsupNfcTag;

	@FXML
	CheckMenuItem buttonDisplayLogs;

	@FXML
	CheckMenuItem buttonDisplayControl;

	@FXML
	Menu camerasMenu;

	@FXML
	Button checkAuth;

	@FXML
	Button checkCamera;

	@FXML
	Button checkNfc;

	@FXML
	Button checkPrinter;

	@FXML
	Button startButton;

	@FXML
	Button stopButton;

	@FXML
	ComboBox<String> comboBox;

	@FXML
	public TextArea logTextarea;

	@FXML
	public Pane nfcTagPane;

	@FXML
	FlowPane statutPane;

	@FXML
	public Pane controlPane;

	@FXML
	Label textPrincipal;

	@FXML
	public ImageView webcamImageView;

	@FXML
	public ImageView bmpBlackImageView;

	@FXML
	public ImageView bmpColorImageView;

	@FXML
	public ImageView bmpBackImageView;

	@FXML
	ProgressBar progressBar;

	@Resource
	WebcamTaskService webcamTaskService;

	@Resource
	EsupSgcTaskServiceFactory esupSgcTaskServiceFactory;

	@Resource
	EsupNfcClientStackPane esupNfcClientStackPane;

	@Resource
	EsupSgcTestPcscDialog esupSgcTestPcscDialog;

	@Resource
	EsupSgcDesfireFullTestPcscDialog esupSgcDesfireFullTestPcscDialog;
	Stage stage;

	@Resource
	AppVersion appVersion;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		logTextarea.textProperty().addListener((observable, oldValue, newValue) -> {
			String value = newValue;
			if (newValue != null && oldValue != null) {
				value = newValue.replace(oldValue, "");
				if (value.contains("\n")) {
					log.info(value.replace("\n", ""));
				}
			}
		});

		logTextarea.appendText("Esup-SGC-Client " + appVersion.getVersion() + " - compilé le " + appVersion.getBuildDate() + "\n");

		esupSgcTaskServiceFactory.init(webcamImageView, bmpColorImageView, bmpBlackImageView, bmpBackImageView, logTextarea, progressBar, textPrincipal, actionsPane, autostart);

		// redimensionnement possible en fonction de la visible
		nfcTagPane.managedProperty().bind(nfcTagPane.visibleProperty());
		logTextarea.managedProperty().bind(logTextarea.visibleProperty());
		statutPane.managedProperty().bind(statutPane.visibleProperty());
		controlPane.managedProperty().bind(controlPane.visibleProperty());

		statutPane.getParent().managedProperty().bind(statutPane.managedProperty().or(controlPane.managedProperty()));

		// affichage panes fonction de la (dé)sélection menus affichage
		nfcTagPane.visibleProperty().bind(buttonDisplayEsupNfcTag.selectedProperty());
		statutPane.visibleProperty().bind(buttonDisplayStatut.selectedProperty());
		logTextarea.visibleProperty().bind(buttonDisplayLogs.selectedProperty());
		controlPane.visibleProperty().bind(buttonDisplayControl.selectedProperty());

		// changement de la visibilité -> redimensionnement effectif de l'application
		nfcTagPane.visibleProperty().addListener(observable -> stage.sizeToScene());
		statutPane.visibleProperty().addListener(observable -> stage.sizeToScene());
		logTextarea.visibleProperty().addListener(observable -> stage.sizeToScene());
		controlPane.visibleProperty().addListener(observable -> stage.sizeToScene());

		// (dé)sélection menu affichage -> sauvegarde dans le filelocalstorage
		buttonDisplayEsupNfcTag.selectedProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("displayEsupNfcTag", newValue.toString()));
		buttonDisplayStatut.selectedProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("displayStatut", newValue.toString()));
		buttonDisplayLogs.selectedProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("displayLogs", newValue.toString()));
		buttonDisplayControl.selectedProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("displayControl", newValue.toString()));
		autostart.selectedProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("autostart", newValue.toString()));

		webcamImageView.managedProperty().bind(webcamImageView.visibleProperty());
		bmpBlackImageView.managedProperty().bind(bmpBlackImageView.visibleProperty());
		bmpColorImageView.managedProperty().bind(bmpColorImageView.visibleProperty());

		webcamImageView.setFitWidth(380.0);
		bmpBlackImageView.setFitWidth(250.0);
		bmpColorImageView.setFitWidth(250.0);

		nfcTagPane.getChildren().add(esupNfcClientStackPane);

		stopButton.disableProperty().bind(appSession.taskIsRunningProperty().not());

		comboBox.disableProperty().bind(appSession.taskIsRunningProperty());

		comboBox.getItems().add("");
		comboBox.getItems().addAll(esupSgcTaskServiceFactory.getServicesNames());

		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldServiceName, newServiceName) -> {
			log.debug("comboBox SelectionModel Event : " + options.getValue() + " - " + oldServiceName + " - " + newServiceName);
			Utils.jfxRunLaterIfNeeded(() -> {
				if (!StringUtils.isEmpty(newServiceName)) {
					if (autostart.isSelected() && !StringUtils.isEmpty(oldServiceName)) {
						esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).removeListener(esupSgcTaskServiceFactory.getStopStartListener(newServiceName));
					}
					esupSgcTaskServiceFactory.resetUiSteps();
					startButton.disableProperty().bind(appSession.taskIsRunningProperty().or(esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).not()));
					fileLocalStorage.setItem("esupsgcTask", newServiceName);
					if (!esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).get()) {
						logTextarea.appendText(String.format("Impossible de démarrer le service '%s' :\n", newServiceName));
						logTextarea.appendText(esupSgcTaskServiceFactory.readyToRunPropertyDisplayProblem(newServiceName));
					} else {
						logTextarea.appendText(String.format("Le service '%s' est prêt à démarrer.\n", newServiceName));
					}
					esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).addListener(esupSgcTaskServiceFactory.getStopStartListener(newServiceName));
					if (esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).get() && autostart.isSelected()) {
						logTextarea.appendText(String.format("Autostart est activé, le service '%s' va démarrer.\n", newServiceName));
						esupSgcTaskServiceFactory.runService(newServiceName);
					}
				} else {
					startButton.disableProperty().unbind();
					startButton.setDisable(true);
				}
			});
		});

		pcscTest.setOnAction(event -> esupSgcTestPcscDialog.getTestPcscDialog(null, null).show());
		pcscTest.disableProperty().bind(appSession.nfcReadyProperty().not().or(appSession.taskIsRunningProperty()));

		pcscDesfireTest.setOnAction(event -> esupSgcDesfireFullTestPcscDialog.getTestPcscDialog(null, null).show());
		pcscDesfireTest.disableProperty().bind(appSession.nfcReadyProperty().not().or(appSession.taskIsRunningProperty()));

		exit.setOnAction(event -> {
			this.exit();
		});

		reinitAndExit.setOnAction(event -> {
			fileLocalStorage.clear();
			this.exit();
		});

		appSession.nfcReadyProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				Utils.jfxRunLaterIfNeeded(() -> {
					if (newValue) {
						checkNfc.getStyleClass().clear();
						checkNfc.getStyleClass().add("btn-success");
						logTextarea.appendText("PC/SC OK\n");
					} else {
						checkNfc.getStyleClass().clear();
						checkNfc.getStyleClass().add("btn-danger");
						logTextarea.appendText("PC/SC KO\n");
					}
				});
			}
		});

		appSession.authReadyProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				Utils.jfxRunLaterIfNeeded(() -> {
					if (newValue) {
						checkAuth.getStyleClass().clear();
						checkAuth.getStyleClass().add("btn-success");
						checkAuth.getTooltip().setText(appSession.eppnInit);
						logTextarea.appendText("Authentification OK : " + appSession.eppnInit + "\n");
					} else {
						checkAuth.getStyleClass().clear();
						checkAuth.getStyleClass().add("btn-danger");
						checkAuth.getTooltip().setText("...");
						logTextarea.appendText("Authentification K0 for " + appSession.eppnInit + " - we refresh iframe on esup-nfc-tag\n");
						esupNfcClientStackPane.init();
					}
				});
			}
		});

		appSession.printerReadyProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				Utils.jfxRunLaterIfNeeded(() -> {
					if (newValue) {
						checkPrinter.getStyleClass().clear();
						checkPrinter.getStyleClass().add("btn-success");
						logTextarea.appendText("imprimante OK\n");
					} else {
						checkPrinter.getStyleClass().clear();
						checkPrinter.getStyleClass().add("btn-danger");
						logTextarea.appendText("imprimante KO\n");
					}
				});
			}
		});

		appSession.webcamReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				Utils.jfxRunLaterIfNeeded(() -> {
					if (newValue) {
						checkCamera.getStyleClass().clear();
						checkCamera.getStyleClass().add("btn-success");
						logTextarea.appendText("Caméra OK.\n");
					} else {
						checkCamera.getStyleClass().clear();
						checkCamera.getStyleClass().add("btn-danger");
						logTextarea.appendText("Caméra déconnectée ?!\n");
					}
				});
			}
		});

		stopButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				esupSgcTaskServiceFactory.cancelService(comboBox.getSelectionModel().getSelectedItem());
			}
		});

		startButton.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				esupSgcTaskServiceFactory.runService(comboBox.getSelectionModel().getSelectedItem());
				Utils.jfxRunLaterIfNeeded(() -> {
					logTextarea.appendText(String.format("Service '%s' démarré.\n", comboBox.getSelectionModel().getSelectedItem()));
				});
			}
		});

		if (esupSgcPrinterService != null) {
			esupSgcPrinterService.setupJfxUi(stage, checkPrinter.getTooltip(), logTextarea, menuBar);
		} else {
			checkPrinter.setDisable(true);
		}

		checkNfc.getTooltip().textProperty().bind(nfcHeartbeatTaskService.titleProperty());
		nfcHeartbeatTaskService.start();
		nfcHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(newValue + "\n")));

		Webcam.addDiscoveryListener(new EsupWebcamDiscoveryListener(this));
		initWebcam(logTextarea);
	}

	// For macOS, this part must be in the main Thread / Static Method
	// With this webcams are discovered and listener works at startup
	static void initWebcam(TextArea logTextarea) {
		try {
			Webcam.getWebcams();
		} catch (Exception e) {
			try{
				log.warn("Webcam discovery failed, try to use NativeDriver ...", e);
				// useful for macOS M1 for example
				Webcam.setDriver(new NativeDriver());
				Webcam.getWebcams();
				log.warn("Webcam discovery success with NativeDriver !", e);
			} catch (Exception ee) {
				log.error("Webcam discovery failed", e);
				logTextarea.appendText("Webcam discovery failed\n");
			}
		}
	}

	public void initializeFromFileLocalStorage(Stage stage) {

		this.stage = stage;

		stage.setTitle("Esup-SGC-Client " + appVersion.getVersion());

		// initialisation (dé)sélection menu affichage fonction du filelocalstorage
		buttonDisplayEsupNfcTag.setSelected(!"false".equals(fileLocalStorage.getItem("displayEsupNfcTag")));
		buttonDisplayStatut.setSelected(!"false".equals(fileLocalStorage.getItem("displayStatut")));
		buttonDisplayLogs.setSelected(!"false".equals(fileLocalStorage.getItem("displayLogs")));
		buttonDisplayControl.setSelected(!"false".equals(fileLocalStorage.getItem("displayControl")));
		autostart.setSelected("true".equals(fileLocalStorage.getItem("autostart")));

		// initialisation tâche combobox après 2 secondes - temps d'initialisation auth/nfc/imprimante ...
		log.info("Tâche au démarrage : " + fileLocalStorage.getItem("esupsgcTask"));
		new Thread(() -> {
			Utils.sleep(2000);
			Utils.jfxRunLaterIfNeeded(() -> {comboBox.getSelectionModel().select(fileLocalStorage.getItem("esupsgcTask"));});
		}).start();
	}

	public synchronized void addWebcamMenuItem(String webcamName) {
		boolean alreadyOk = false;
		boolean webcamSelected = false;
		for(MenuItem menuItem : camerasMenu.getItems()) {
			if(menuItem.getText().equals(webcamName)) {
				alreadyOk = true;
			}
			if(((CheckMenuItem)menuItem).isSelected()) {
				webcamSelected = true;
			}
		}
		if(!alreadyOk) {
			CheckMenuItem webcamMenuItem = new CheckMenuItem();
			webcamMenuItem.setText(webcamName);
			webcamMenuItem.selectedProperty().addListener((obs, wasSelected, isSelected) -> {
				if(isSelected) {
					String newWebcamName = webcamName;
					if (webcamTaskService != null && webcamTaskService.isRunning()) {
						webcamTaskService.cancel();
					}
					webcamTaskService.init(newWebcamName, webcamImageView);
					webcamTaskService.restart();
					Utils.jfxRunLaterIfNeeded(() -> {
						checkCamera.getTooltip().setText(newWebcamName);
						for (MenuItem menuItem : camerasMenu.getItems()) {
							if (!menuItem.getText().equals(newWebcamName) && ((CheckMenuItem) menuItem).isSelected()) {
								((CheckMenuItem) menuItem).setSelected(false);
								menuItem.setDisable(false);
							}
						}
						webcamMenuItem.setDisable(true);
					});
				}
			});
			Utils.jfxRunLaterIfNeeded(() -> {
				camerasMenu.getItems().add(webcamMenuItem);
			});
		}
		if(!webcamSelected && camerasMenu.getItems().size()>0) {
			Utils.jfxRunLaterIfNeeded(() -> {
				((CheckMenuItem) camerasMenu.getItems().get(0)).selectedProperty().setValue(true);
			});
		}
	}
	public synchronized void removeWebcamMenuItem(String webcamName) {
		MenuItem webcamMenuItem = null;
		for(MenuItem menuItem : camerasMenu.getItems()) {
			if(menuItem.getText().equals(webcamName)) {
				webcamMenuItem = menuItem;
				break;
			}
		}
		if(webcamMenuItem != null) {
			final MenuItem webcamMenuItem2remove = webcamMenuItem;
			Utils.jfxRunLaterIfNeeded(() -> {
				camerasMenu.getItems().remove(webcamMenuItem2remove);
			});
		}
	}

	public void exit() {
		logTextarea.appendText("Arrêt demandé\n");
		esupSgcTaskServiceFactory.cancelService(comboBox.getSelectionModel().getSelectedItem());
		webcamTaskService.cancel();
		nfcHeartbeatTaskService.cancel();
		stage.close();
		Platform.exit();
		System.exit(0);
	}
}
