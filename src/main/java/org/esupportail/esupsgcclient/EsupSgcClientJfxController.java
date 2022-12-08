package org.esupportail.esupsgcclient;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.CheckMenuItem;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuItem;
import javafx.scene.control.ProgressBar;
import javafx.scene.layout.FlowPane;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.NfcHeartbeatTaskService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisHeartbeatTaskService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;
import org.esupportail.esupsgcclient.service.webcam.EsupWebcamDiscoveryListener;
import org.esupportail.esupsgcclient.service.webcam.WebcamTaskService;

import com.github.sarxos.webcam.Webcam;

import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;
import java.net.URL;
import java.util.ResourceBundle;

@Component
public class EsupSgcClientJfxController implements Initializable {

	final static Logger log = Logger.getLogger(EsupSgcClientJfxController.class);

	@Resource
	AppSession appSession;

	@Resource
	EvolisPrinterService evolisPrinterService;

	@Resource
	EvolisHeartbeatTaskService evolisHeartbeatTaskService;

	@Resource
	NfcHeartbeatTaskService nfcHeartbeatTaskService;

	@FXML
	private FlowPane actionsPane;

	@FXML
	private CheckMenuItem buttonDisplayStatut;

	@FXML
	private CheckMenuItem buttonDisplayEsupNfcTag;

	@FXML
	private CheckMenuItem buttonDisplayLogs;

	@FXML
	private CheckMenuItem buttonDisplayControl;

	@FXML
	private MenuItem evolisReject;

	@FXML
	private MenuItem evolisPrintEnd;

	@FXML
	private Menu camerasMenu;

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
	private FlowPane statutPane;

	@FXML
	public Pane controlPane;

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

	@FXML
	private Pane restartButtons;

	@Resource
	WebcamTaskService webcamTaskService;

	@Resource
	EsupSgcTaskServiceFactory esupSgcTaskServiceFactory;

	@Resource
	EsupNfcClientStackPane esupNfcClientStackPane;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		esupSgcTaskServiceFactory.init(webcamImageView, bmpColorImageView, bmpBlackImageView, logTextarea, progressBar, textPrincipal, actionsPane, restartButtons);

		nfcTagPane.managedProperty().bind(nfcTagPane.visibleProperty());
		logTextarea.managedProperty().bind(logTextarea.visibleProperty());
		statutPane.managedProperty().bind(statutPane.visibleProperty());
		controlPane.managedProperty().bind(controlPane.visibleProperty());

		nfcTagPane.visibleProperty().bind(buttonDisplayEsupNfcTag.selectedProperty());
		statutPane.visibleProperty().bind(buttonDisplayStatut.selectedProperty());
		logTextarea.visibleProperty().bind(buttonDisplayLogs.selectedProperty());
		controlPane.visibleProperty().bind(buttonDisplayControl.selectedProperty());

		nfcTagPane.visibleProperty().addListener(observable -> EsupSgcClientApplication.getPrimaryStage().sizeToScene());
		statutPane.visibleProperty().addListener(observable -> EsupSgcClientApplication.getPrimaryStage().sizeToScene());
		logTextarea.visibleProperty().addListener(observable -> EsupSgcClientApplication.getPrimaryStage().sizeToScene());
		controlPane.visibleProperty().addListener(observable -> EsupSgcClientApplication.getPrimaryStage().sizeToScene());
		
		webcamImageView.managedProperty().bind(webcamImageView.visibleProperty());
		bmpBlackImageView.managedProperty().bind(bmpBlackImageView.visibleProperty());
		bmpColorImageView.managedProperty().bind(bmpColorImageView.visibleProperty());

		nfcTagPane.getChildren().add(esupNfcClientStackPane);

		comboBox.getItems().addAll(esupSgcTaskServiceFactory.getServicesNames());

		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldServiceName, newServiceName) -> {
			log.debug("comboBox SelectionModel Event : " + options.getValue() + " - " +  oldServiceName + " - " + newServiceName);
			if(options.getValue()!=null && newServiceName!=null && !newServiceName.equals(oldServiceName)) {
				Platform.runLater(() -> {
					if(esupSgcTaskServiceFactory.isReadyToRun(appSession, newServiceName)) {
						esupSgcTaskServiceFactory.cancelService(oldServiceName);
						esupSgcTaskServiceFactory.runService(newServiceName);
						logTextarea.appendText(String.format("Service '%s' démarré.\n", newServiceName));
					} else {
						comboBox.getSelectionModel().select(oldServiceName);
						logTextarea.appendText(String.format("Impossible de démarrer le service '%s' actuellement.\n", newServiceName));
					}
				});
			}
		});

		appSession.getNfcReady().addListener(new ChangeListener<Boolean>() {
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

		appSession.getAuthReady().addListener(new ChangeListener<Boolean>() {
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
				}
			}
		});
		evolisReject.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				Thread th = new Thread(new Task<>() {
					@Override
					protected Object call() throws Exception {
						evolisPrinterService.reject();
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
						evolisPrinterService.printEnd();
						return null;
					}
				});
				th.setDaemon(true);
				th.start();
			}
		});

		appSession.getPrinterReady().addListener(new ChangeListener<Boolean>() {
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


		checkPrinter.getTooltip().textProperty().bind(evolisHeartbeatTaskService.titleProperty());
		evolisHeartbeatTaskService.start();
		evolisHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));

		checkNfc.getTooltip().textProperty().bind(nfcHeartbeatTaskService.titleProperty());
		nfcHeartbeatTaskService.start();
		nfcHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));

		Webcam.addDiscoveryListener(new EsupWebcamDiscoveryListener(this));
		Webcam.getWebcams(); // with this webcams are discovered and listener works at startup

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
