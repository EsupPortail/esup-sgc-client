package org.esupportail.esupsgcclient;

import ch.qos.logback.classic.Level;
import com.github.eduramiba.webcamcapture.drivers.NativeDriver;
import com.github.sarxos.webcam.Webcam;
import ch.qos.logback.classic.LoggerContext;
import ch.qos.logback.core.Appender;
import ch.qos.logback.core.FileAppender;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;
import javafx.scene.text.Text;
import javafx.stage.FileChooser;
import javafx.stage.Stage;
import org.apache.commons.lang3.StringUtils;
import org.esupportail.esupsgcclient.service.pcsc.NfcHeartbeatTaskService;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.service.webcam.EsupWebcamDiscoveryListener;
import org.esupportail.esupsgcclient.service.webcam.WebcamTaskService;
import org.esupportail.esupsgcclient.ui.*;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.awt.Desktop;
import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.util.Iterator;
import java.util.ResourceBundle;
import java.util.concurrent.atomic.AtomicLong;

@Component
public class EsupSgcClientJfxController implements Initializable {

	final static Logger log = LoggerFactory.getLogger(EsupSgcClientJfxController.class);

	@Resource
	AppSession appSession;

	@Autowired(required = false)
	EsupSgcPrinterService esupSgcPrinterService;

	@Resource
	NfcHeartbeatTaskService nfcHeartbeatTaskService;

	@Resource
	FileLocalStorage fileLocalStorage;

	private static final String ESUP_LOG_LEVEL_STORAGE_KEY = "esupLogLevel";
	private static final String GENERAL_LOG_LEVEL_STORAGE_KEY = "generalLogLevel";

	@FXML
	CheckMenuItem autostart;

	@FXML
	Menu esupLogMenu;

	@FXML
	Menu generalLogMenu;

	@FXML
	MenuItem reinitAndExit;

	@FXML
	MenuItem pcscTest;

	@FXML
	MenuItem pcscDesfireTest;

	@FXML
	MenuItem exit;

	@FXML
	MenuItem downloadLogFile;

	@FXML
	MenuItem tailLogFile;

	@FXML
	MenuBar menuBar;

	@FXML
	Pane actionsPane;

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
	Label infoText;

	@FXML
	ComboBox<String> comboBox;

	@FXML
	public TextArea logTextarea;

	@FXML
	public SplitPane mainPane;

	@FXML
	public SplitPane mainPane2;

	@FXML
	public SplitPane mainPane3;

	@FXML
	Pane statutPane;

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

    @Resource
    AppConfig appConfig;

	@Resource
	LogTextAreaService logTextAreaService;

	private void initEsupLogLevelMenu() {
		initLogLevelMenu(esupLogMenu, ESUP_LOG_LEVEL_STORAGE_KEY, LogLevelManager.ESUP_LOGGER_NAME);
	}

	private void initGeneralLogLevelMenu() {
		initLogLevelMenu(generalLogMenu, GENERAL_LOG_LEVEL_STORAGE_KEY, org.slf4j.Logger.ROOT_LOGGER_NAME);
	}

	private void initLogLevelMenu(Menu menu, String storageKey, String loggerName) {
		ToggleGroup toggleGroup = new ToggleGroup();
		LogLevelManager.LogLevelChoice savedChoice = LogLevelManager.LogLevelChoice.fromName(fileLocalStorage.getItem(storageKey));
		menu.getItems().clear();

		for (LogLevelManager.LogLevelChoice choice : LogLevelManager.LogLevelChoice.values()) {
			RadioMenuItem item = new RadioMenuItem(choice.getLabel());
			item.setToggleGroup(toggleGroup);
			if (choice == savedChoice) {
				item.setSelected(true);
			}
			item.setOnAction(event -> applyLogLevelChoice(loggerName, choice, storageKey));
			menu.getItems().add(item);
		}

		applyLogLevelChoice(loggerName, savedChoice, storageKey);
	}

	private void applyLogLevelChoice(String loggerName, LogLevelManager.LogLevelChoice choice, String storageKey) {
		if (org.slf4j.Logger.ROOT_LOGGER_NAME.equals(loggerName)) {
			LogLevelManager.setRootLoggerLevel(choice.getLevel());
		} else {
			LogLevelManager.setLoggerLevel(loggerName, choice.getLevel());
		}
		fileLocalStorage.setItem(storageKey, choice.getLabel());
	}

	private File resolveLogFile() {
		String logPath = System.getProperty("LOG_PATH");
		if (StringUtils.isNotBlank(logPath)) {
			return new File(logPath);
		}

		LoggerContext context = (LoggerContext) LoggerFactory.getILoggerFactory();
		for (ch.qos.logback.classic.Logger logger : context.getLoggerList()) {
			Iterator<Appender<ch.qos.logback.classic.spi.ILoggingEvent>> it = logger.iteratorForAppenders();
			while (it.hasNext()) {
				Appender<ch.qos.logback.classic.spi.ILoggingEvent> appender = it.next();
				if (appender instanceof FileAppender) {
					File file = new File(((FileAppender<?>) appender).getFile());
					if (file.isAbsolute()) {
						return file;
					}
					return new File(System.getProperty("user.dir"), file.getPath());
				}
			}
		}
		return new File("esupsgcclient.log");
	}

	private void downloadLogFile() {
		File logFile = resolveLogFile();
		if (!logFile.exists()) {
			logTextAreaService.appendText("Le fichier de log n'existe pas : " + logFile.getAbsolutePath());
			return;
		}

		FileChooser chooser = new FileChooser();
		chooser.setTitle("Télécharger le fichier de log");
		chooser.setInitialFileName(logFile.getName());
		File target = chooser.showSaveDialog(stage);
		if (target != null) {
			try {
				Files.copy(logFile.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING);
				logTextAreaService.appendText("Fichier de log téléchargé vers : " + target.getAbsolutePath());
			} catch (IOException e) {
				log.error("Erreur lors du téléchargement du fichier de log {}", logFile.getAbsolutePath(), e);
				logTextAreaService.appendText("Erreur lors du téléchargement du fichier de log");
			}
		}
	}

	private void openLiveLogWindow() {
		File logFile = resolveLogFile();
		Stage dialog = new Stage();
		dialog.setTitle("Suivi des logs en direct");
		if (stage != null) {
			dialog.initOwner(stage);
		}

		TextArea liveLogArea = new TextArea();
		liveLogArea.setEditable(false);
		liveLogArea.setWrapText(true);
		VBox root = new VBox(10, liveLogArea);
		root.setPadding(new Insets(10));
		dialog.setScene(new Scene(root, 900, 500));

		AtomicLong filePointer = new AtomicLong(logFile.exists() ? logFile.length() : 0L);
		Thread tailThread = new Thread(() -> {
			while (!Thread.currentThread().isInterrupted()) {
				try {
					if (logFile.exists()) {
						try (RandomAccessFile raf = new RandomAccessFile(logFile, "r")) {
							if (filePointer.get() > raf.length()) {
								filePointer.set(0L);
							}
							raf.seek(filePointer.get());
							String line;
							while ((line = raf.readLine()) != null) {
								String decodedLine = new String(line.getBytes("ISO-8859-1"), "UTF-8");
								String text = decodedLine + System.lineSeparator();
								Platform.runLater(() -> liveLogArea.appendText(text));
								filePointer.set(raf.getFilePointer());
							}
						}
					}
					Thread.sleep(500L);
				} catch (InterruptedException e) {
					Thread.currentThread().interrupt();
					break;
				} catch (IOException e) {
					log.error("Erreur de lecture du fichier de log {}", logFile.getAbsolutePath(), e);
					Platform.runLater(() -> liveLogArea.appendText("Erreur de lecture du fichier de log.\n"));
					try {
						Thread.sleep(2000L);
					} catch (InterruptedException interruptedException) {
						Thread.currentThread().interrupt();
						break;
					}
				}
			}
		}, "esup-log-tail");
		tailThread.setDaemon(true);
		dialog.setOnCloseRequest(event -> tailThread.interrupt());
		dialog.show();
		tailThread.start();
	}

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {
		initGeneralLogLevelMenu();
		initEsupLogLevelMenu();

		logTextAreaService.initLogTextArea(logTextarea, infoText);

		logTextAreaService.appendText("Esup-SGC-Client " + appVersion.getVersion() + " - compilé le " + appVersion.getBuildDate());
		logTextAreaService.appendText("ESUP-SGC : " + appConfig.getEsupSgcUrl());
		logTextAreaService.appendText("Esup-NFC-Tag-Server : " + appConfig.getEsupNfcTagServerUrl());

		esupSgcTaskServiceFactory.init(webcamImageView, bmpColorImageView, bmpBlackImageView, bmpBackImageView, progressBar, textPrincipal, actionsPane, autostart);

		esupNfcClientStackPane.init();
		mainPane.getItems().add(0, esupNfcClientStackPane);

		// redimensionnement possible en fonction de la visible
		esupNfcClientStackPane.managedProperty().bind(esupNfcClientStackPane.visibleProperty());
		logTextarea.managedProperty().bind(logTextarea.visibleProperty());
		statutPane.managedProperty().bind(statutPane.visibleProperty());
		controlPane.managedProperty().bind(controlPane.visibleProperty());

		// changement de la visibilité -> redimensionnement effectif de l'application
		esupNfcClientStackPane.visibleProperty().addListener(observable -> stage.sizeToScene());
		statutPane.visibleProperty().addListener(observable -> stage.sizeToScene());
		logTextarea.visibleProperty().addListener(observable -> stage.sizeToScene());
		controlPane.visibleProperty().addListener(observable -> stage.sizeToScene());

		// (dé)sélection menu autostart -> sauvegarde dans le filelocalstorage
		autostart.selectedProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("autostart", newValue.toString()));

		webcamImageView.managedProperty().bind(webcamImageView.visibleProperty());
		bmpBlackImageView.managedProperty().bind(bmpBlackImageView.visibleProperty());
		bmpColorImageView.managedProperty().bind(bmpColorImageView.visibleProperty());

		stopButton.disableProperty().bind(appSession.taskIsRunningProperty().not());

		comboBox.disableProperty().bind(appSession.taskIsRunningProperty());

		comboBox.getItems().add("");
		comboBox.getItems().addAll(esupSgcTaskServiceFactory.getServicesNames());

		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldServiceName, newServiceName) -> {
			log.debug("comboBox SelectionModel Event : " + options.getValue() + " - " + oldServiceName + " - " + newServiceName);
			Utils.jfxRunLaterIfNeeded(() -> {
				if (!StringUtils.isEmpty(newServiceName) && esupSgcTaskServiceFactory.getServicesNames().contains(newServiceName)) {
					if (autostart.isSelected() && !StringUtils.isEmpty(oldServiceName)) {
						esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).removeListener(esupSgcTaskServiceFactory.getStopStartListener(newServiceName));
					}
					esupSgcTaskServiceFactory.resetUiSteps();
					startButton.disableProperty().bind(appSession.taskIsRunningProperty().or(esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).not()));
					fileLocalStorage.setItem("esupsgcTask", newServiceName);
					if (!esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).get()) {
						logTextAreaService.appendText(String.format("Impossible de démarrer le service '%s' :", newServiceName));
						logTextAreaService.appendText(esupSgcTaskServiceFactory.readyToRunPropertyDisplayProblem(newServiceName));
					} else {
						logTextAreaService.appendText(String.format("Le service '%s' est prêt à démarrer.", newServiceName));
					}
					esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).addListener(esupSgcTaskServiceFactory.getStopStartListener(newServiceName));
					if (esupSgcTaskServiceFactory.readyToRunProperty(newServiceName).get() && autostart.isSelected()) {
						logTextAreaService.appendText(String.format("Autostart est activé, le service '%s' va démarrer.", newServiceName));
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

		downloadLogFile.setOnAction(event -> downloadLogFile());
		tailLogFile.setOnAction(event -> openLiveLogWindow());

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
						logTextAreaService.appendText("PC/SC OK");
					} else {
						checkNfc.getStyleClass().clear();
						checkNfc.getStyleClass().add("btn-danger");
						logTextAreaService.appendText("PC/SC KO");
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
						logTextAreaService.appendText("Authentification OK : " + appSession.eppnInit);
					} else {
						checkAuth.getStyleClass().clear();
						checkAuth.getStyleClass().add("btn-danger");
						checkAuth.getTooltip().setText("...");
						logTextAreaService.appendText("Authentification K0 for " + appSession.eppnInit + " - we refresh iframe on esup-nfc-tag");
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
						logTextAreaService.appendText("imprimante OK");
					} else {
						checkPrinter.getStyleClass().clear();
						checkPrinter.getStyleClass().add("btn-danger");
						logTextAreaService.appendText("imprimante KO");
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
						logTextAreaService.appendText("Caméra OK.");
					} else {
						checkCamera.getStyleClass().clear();
						checkCamera.getStyleClass().add("btn-danger");
						logTextAreaService.appendText("Caméra déconnectée ?!");
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
					logTextAreaService.appendText(String.format("Service '%s' démarré.", comboBox.getSelectionModel().getSelectedItem()));
				});
			}
		});

		if (esupSgcPrinterService != null) {
			esupSgcPrinterService.setupJfxUi(stage, checkPrinter.getTooltip(), menuBar);
		} else {
			checkPrinter.setDisable(true);
		}

		checkNfc.getTooltip().textProperty().bind(nfcHeartbeatTaskService.titleProperty());
		nfcHeartbeatTaskService.start();
		nfcHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> Utils.jfxRunLaterIfNeeded(() -> logTextAreaService.appendText(newValue)));

		Webcam.addDiscoveryListener(new EsupWebcamDiscoveryListener(this));
		initWebcam(logTextAreaService);

		// responsive : largeur des images == largeur du pane parent
		((Pane)webcamImageView.getParent()).widthProperty().addListener(new ChangeListener<Number>() {
			@Override
			public void changed(ObservableValue<? extends Number> observableValue, Number number, Number t1) {
				double width = ((Pane)webcamImageView.getParent()).getWidth();
				webcamImageView.setFitWidth(width);
				bmpBlackImageView.setFitWidth(width);
				bmpColorImageView.setFitWidth(width);
				bmpBackImageView.setFitWidth(width);
			}
		});

	}

	// For macOS, this part must be in the main Thread / Static Method
	// With this webcams are discovered and listener works at startup
	static void initWebcam(LogTextAreaService logTextAreaService) {
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
				logTextAreaService.appendText("Webcam discovery failed");
			}
		}
	}

	public void initializeFromFileLocalStorage(Stage stage) {

		this.stage = stage;

		stage.setTitle("Esup-SGC-Client " + appVersion.getVersion());

		// initialisation (dé)sélection menu autostart fonction du filelocalstorage
		autostart.setSelected("true".equals(fileLocalStorage.getItem("autostart")));

		// initialisation position fenêtre fonction du filelocalstorage
		if(!StringUtils.isEmpty(fileLocalStorage.getItem("stageX")) && !StringUtils.isEmpty(fileLocalStorage.getItem("stageY"))) {
			stage.setX(Double.parseDouble(fileLocalStorage.getItem("stageX")));
			stage.setY(Double.parseDouble(fileLocalStorage.getItem("stageY")));
		}
		// application en plein écran par défaut - on préserve la taille de la fenêtre si elle n'est pas maximisée
		if(!StringUtils.isEmpty(fileLocalStorage.getItem("sizeWidth")) && !StringUtils.isEmpty(fileLocalStorage.getItem("sizeHeight"))) {
			stage.setWidth(Double.parseDouble(fileLocalStorage.getItem("sizeWidth")));
			stage.setHeight(Double.parseDouble(fileLocalStorage.getItem("sizeHeight")));
		}
		stage.setMaximized(true);
		// now add eventlistener stage x, y -> sauvegarde dans le filelocalstorage
		stage.xProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("stageX", newValue.toString()));
		stage.yProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("stageY", newValue.toString()));
		// now add eventlistener stage dimension -> sauvegarde dans le filelocalstorage
		stage.widthProperty().addListener((observableValue, oldValue, newValue) -> {
			if(!stage.isMaximized()) {
				fileLocalStorage.setItem("sizeWidth", newValue.toString());
			}
		});
		stage.heightProperty().addListener((observableValue, oldValue, newValue) -> {
			if(!stage.isMaximized()) {
				fileLocalStorage.setItem("sizeHeight", newValue.toString());
			}
		});

		//initialisation splitpane fonction du filelocalstorage
		double mainPaneDivider = 0.25;
		double mainPane2Divider = 0.7;
		double mainPane3Divider = 0.7;
		if(!StringUtils.isEmpty(fileLocalStorage.getItem("mainPaneDivider"))) {
			mainPaneDivider = Double.parseDouble(fileLocalStorage.getItem("mainPaneDivider"));
		}
		if(!StringUtils.isEmpty(fileLocalStorage.getItem("mainPane2Divider"))) {
			mainPane2Divider = Double.parseDouble(fileLocalStorage.getItem("mainPane2Divider"));
		}
		if(!StringUtils.isEmpty(fileLocalStorage.getItem("mainPane3Divider"))) {
			mainPane3Divider = Double.parseDouble(fileLocalStorage.getItem("mainPane3Divider"));
		}
		mainPane.setDividerPositions(mainPaneDivider);
		mainPane2.setDividerPositions(mainPane2Divider);
		mainPane3.setDividerPositions(mainPane3Divider);

		// now add eventlistener splitpane dimension -> sauvegarde dans le filelocalstorage
		mainPane.getDividers().get(0).positionProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("mainPaneDivider", newValue.toString()));
		mainPane2.getDividers().get(0).positionProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("mainPane2Divider", newValue.toString()));
		mainPane3.getDividers().get(0).positionProperty().addListener((observableValue, oldValue, newValue) -> fileLocalStorage.setItem("mainPane3Divider", newValue.toString()));

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
		logTextAreaService.appendText("Arrêt demandé");
		esupSgcTaskServiceFactory.cancelService(comboBox.getSelectionModel().getSelectedItem());
		webcamTaskService.cancel();
		nfcHeartbeatTaskService.cancel();
		stage.close();
		Platform.exit();
		System.exit(0);
	}
}
