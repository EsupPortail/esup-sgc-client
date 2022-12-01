package org.esupportail.esupsgcclient;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Task;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
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

	@Resource
	WebcamTaskService webcamTaskService;

	@Resource
	EsupSgcTaskServiceFactory esupSgcTaskServiceFactory;

	@Resource
	EsupNfcClientStackPane esupNfcClientStackPane;

	@Override
	public void initialize(URL url, ResourceBundle resourceBundle) {

		esupSgcTaskServiceFactory.init(webcamImageView, bmpColorImageView, bmpBlackImageView, logTextarea, progressBar, textPrincipal, actionsPane);

		logTextarea.managedProperty().bind(logTextarea.visibleProperty());
		nfcTagPane.managedProperty().bind(nfcTagPane.visibleProperty());
		webcamImageView.managedProperty().bind(webcamImageView.visibleProperty());
		bmpBlackImageView.managedProperty().bind(bmpBlackImageView.visibleProperty());
		bmpColorImageView.managedProperty().bind(bmpColorImageView.visibleProperty());

		nfcTagPane.getChildren().add(esupNfcClientStackPane);
		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldWebcamName, newWebcamName) -> {
			log.debug("comboBox SelectionModel Event : " + options.getValue() + " - " +  oldWebcamName + " - " + newWebcamName);
			if(options.getValue()!=null && newWebcamName!=null && !newWebcamName.equals(oldWebcamName)) {
				if(webcamTaskService != null && webcamTaskService.isRunning()) {
					webcamTaskService.cancel();
				}
				webcamTaskService.init(newWebcamName, webcamImageView);
				webcamTaskService.restart();
				checkCamera.getTooltip().setText(newWebcamName);
			}
		});

		buttonLogs.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				if (logTextarea.isVisible()) {
					logTextarea.setVisible(false);
					buttonLogs.setText("Afficher les logs");
				} else {
					logTextarea.setVisible(true);
					buttonLogs.setText("Masquer les logs");
				}
				EsupSgcClientApplication.getPrimaryStage().sizeToScene();
			}
		});

		buttonNfcTag.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				if (nfcTagPane.isVisible()) {
					nfcTagPane.setVisible(false);
					buttonNfcTag.setText("Afficher EsupNfcTag");
				} else {
					nfcTagPane.setVisible(true);
					buttonNfcTag.setText("Masquer EsupNfcTag");
				}
				EsupSgcClientApplication.getPrimaryStage().sizeToScene();
			}
		});


		appSession.getNfcReady().addListener(new ChangeListener<Boolean>() {
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

		appSession.getAuthReady().addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkAuth.getStyleClass().clear();
					checkAuth.getStyleClass().add("btn-success");
					checkAuth.getTooltip().setText(appSession.eppnInit);
					logTextarea.appendText("Authentification OK : " + appSession.eppnInit + "\n");
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
					bmpBlackImageView.setVisible(true);
					bmpColorImageView.setVisible(true);
					// webcamImageView.setVisible(false);
					// primaryStage.sizeToScene();
					startLoopServiceIfPossible();
				} else {
					checkPrinter.getStyleClass().clear();
					checkPrinter.getStyleClass().add("btn-danger");
				}
			}
		});


		appSession.webcamReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkCamera.getStyleClass().clear();
					checkCamera.getStyleClass().add("btn-success");
					webcamImageView.setVisible(true);
					startLoopServiceIfPossible();
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
		if(appSession.isAuthReady() && appSession.isNfcReady() && appSession.isWebcamReady()) {
			Platform.runLater(() -> {
					esupSgcTaskServiceFactory.runQrCodeTaskService();
					logTextarea.appendText("qrCodeTaskService is now running\n");
			});
		}
		if(appSession.isAuthReady() && appSession.isNfcReady() && appSession.isPrinterReady()) {
			Platform.runLater(() -> {
					esupSgcTaskServiceFactory.runEvolisTaskService();
					logTextarea.appendText("evolisEsupSgcLongPollTaskService is now running\n");
			});
		}
	}


}
