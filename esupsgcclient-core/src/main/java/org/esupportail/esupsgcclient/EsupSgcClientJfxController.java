package org.esupportail.esupsgcclient;

import com.github.sarxos.webcam.Webcam;
import jakarta.annotation.Resource;
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
	ProgressBar progressBar;

	@Resource
	WebcamTaskService webcamTaskService;

	@Resource
	EsupSgcTaskServiceFactory esupSgcTaskServiceFactory;

	@Resource
	EsupNfcClientStackPane esupNfcClientStackPane;

	Stage stage;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		esupSgcTaskServiceFactory.init(webcamImageView, bmpColorImageView, bmpBlackImageView, logTextarea, progressBar, textPrincipal, actionsPane, autostart);

		// redimensionnement possible en fonction de la visible
		nfcTagPane.managedProperty().bind(nfcTagPane.visibleProperty());
		logTextarea.managedProperty().bind(logTextarea.visibleProperty());
		statutPane.managedProperty().bind(statutPane.visibleProperty());
		controlPane.managedProperty().bind(controlPane.visibleProperty());

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

		textPrincipal.setWrapText(true);
		textPrincipal.setMaxSize(500, 1);

		nfcTagPane.getChildren().add(esupNfcClientStackPane);

		stopButton.disableProperty().bind(appSession.taskIsRunningProperty().not());

		comboBox.disableProperty().bind(appSession.taskIsRunningProperty());

		comboBox.getItems().add("");
		comboBox.getItems().addAll(esupSgcTaskServiceFactory.getServicesNames());

		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldServiceName, newServiceName) -> {
			log.debug("comboBox SelectionModel Event : " + options.getValue() + " - " +  oldServiceName + " - " + newServiceName);
			if(!StringUtils.isEmpty(newServiceName)) {
				Platform.runLater(() -> {
					if(autostart.isSelected() && !StringUtils.isEmpty(oldServiceName)) {
						esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).removeListener(esupSgcTaskServiceFactory.getStopStartListener(newServiceName));
					}
					esupSgcTaskServiceFactory.resetUiSteps();
					startButton.disableProperty().bind(appSession.taskIsRunningProperty().or(esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).not()));
					fileLocalStorage.setItem("esupsgcTask", newServiceName);
					if(!esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).get()) {
						logTextarea.appendText(String.format("Impossible de démarrer le service '%s' :\n", newServiceName));
						logTextarea.appendText(esupSgcTaskServiceFactory.readyToRunPropertyDisplayProblem(newServiceName));
					} else {
						logTextarea.appendText(String.format("Le service '%s' est prêt à démarrer.\n", newServiceName));
					}
					esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).addListener(esupSgcTaskServiceFactory.getStopStartListener(newServiceName));
					if(esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).get() && autostart.isSelected()) {
						logTextarea.appendText(String.format("Autostart est activé, le service '%s' va démarrer.\n", newServiceName));
						esupSgcTaskServiceFactory.runService(newServiceName);
					}
				});
			} else {
				startButton.disableProperty().unbind();
				startButton.setDisable(true);
			}
		});

		exit.setOnAction(event -> stage.close());

		reinitAndExit.setOnAction(event -> {
			fileLocalStorage.clear();
			stage.close();
		});

		appSession.nfcReadyProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkNfc.getStyleClass().clear();
					checkNfc.getStyleClass().add("btn-success");
					logTextarea.appendText("PC/SC OK\n");
				} else {
					checkNfc.getStyleClass().clear();
					checkNfc.getStyleClass().add("btn-danger");
					logTextarea.appendText("PC/SC KO\n");
				}
			}
		});

		appSession.authReadyProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
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
			}
		});

		appSession.printerReadyProperty().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkPrinter.getStyleClass().clear();
					checkPrinter.getStyleClass().add("btn-success");
					logTextarea.appendText("imprimante evolis OK\n");
				} else {
					checkPrinter.getStyleClass().clear();
					checkPrinter.getStyleClass().add("btn-danger");
					logTextarea.appendText("imprimante evolis KO\n");
				}
			}
		});

		appSession.webcamReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkCamera.getStyleClass().clear();
					checkCamera.getStyleClass().add("btn-success");
					logTextarea.appendText("Caméra OK.\n");
				} else {
					checkCamera.getStyleClass().clear();
					checkCamera.getStyleClass().add("btn-danger");
					logTextarea.appendText("Caméra déconnectée ?!\n");
				}
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
				logTextarea.appendText(String.format("Service '%s' démarré.\n", comboBox.getSelectionModel().getSelectedItem()));
			}
		});

		if(esupSgcPrinterService != null) {
			esupSgcPrinterService.setupJfxUi(checkPrinter.getTooltip(), logTextarea, menuBar);
		} else {
			checkPrinter.setDisable(true);
		}

		checkNfc.getTooltip().textProperty().bind(nfcHeartbeatTaskService.titleProperty());
		nfcHeartbeatTaskService.start();
		nfcHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));

		Webcam.addDiscoveryListener(new EsupWebcamDiscoveryListener(this));
		Webcam.getWebcams(); // with this webcams are discovered and listener works at startup

	}

	public void initializeFromFileLocalStorage(Stage stage) {

		this.stage = stage;

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
			Platform.runLater(() -> {comboBox.getSelectionModel().select(fileLocalStorage.getItem("esupsgcTask"));});
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
					checkCamera.getTooltip().setText(newWebcamName);
					for (MenuItem menuItem : camerasMenu.getItems()) {
						if (!menuItem.getText().equals(newWebcamName) && ((CheckMenuItem) menuItem).isSelected()) {
							((CheckMenuItem) menuItem).setSelected(false);
							menuItem.setDisable(false);
						}
					}
					webcamMenuItem.setDisable(true);
				}
			});
			camerasMenu.getItems().add(webcamMenuItem);
		}
		if(!webcamSelected && camerasMenu.getItems().size()>0) {
			((CheckMenuItem)camerasMenu.getItems().get(0)).selectedProperty().setValue(true);
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
			camerasMenu.getItems().remove(webcamMenuItem);
		}
	}

}
