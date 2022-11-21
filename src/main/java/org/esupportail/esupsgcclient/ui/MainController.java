package org.esupportail.esupsgcclient.ui;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.scene.control.MenuItem;
import javafx.scene.layout.FlowPane;
import javafx.stage.Stage;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.CnousEncodingTaskService;
import org.esupportail.esupsgcclient.service.EsupSgcGetBmpTaskService;
import org.esupportail.esupsgcclient.service.QrCodeTaskService;
import org.esupportail.esupsgcclient.service.pcsc.EncodingService;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisHeartbeatTask;
import org.esupportail.esupsgcclient.service.webcam.EsupWebcamDiscoveryListener;
import org.esupportail.esupsgcclient.task.EncodingTaskService;
import org.esupportail.esupsgcclient.task.EsupSgcLongPollTaskService;
import org.esupportail.esupsgcclient.task.EvolisEjectTaskService;
import org.esupportail.esupsgcclient.task.EvolisPrintTaskService;
import org.esupportail.esupsgcclient.task.WaitRemoveCardTaskService;
import org.esupportail.esupsgcclient.task.WebcamUiTask;

import com.github.sarxos.webcam.Webcam;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.Pane;
import org.esupportail.esupsgcclient.utils.Utils;

public class MainController {

	final static Logger log = Logger.getLogger(MainController.class);

	public enum StyleLevel {success, danger, warning, primary, info};

	private Thread threadWebcamStream = null;

	public Stage primaryStage;

	@FXML
	private MenuItem buttonLogs;

	@FXML
	private MenuItem buttonNfcTag;

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
	private Pane mainPane;

	@FXML
	public Pane nfcTagPane;

	@FXML
	private Label stepClientReady;

	@FXML
	private Label stepReadQR;

	@FXML
	private Label stepReadCSN;

	@FXML
	private Label stepSelectSGC;

	@FXML
	private Label stepEncodageApp;

	@FXML
	private FlowPane panePrincipal;

	@FXML
	private Label textPrincipal;

	@FXML
	private Label title;

	@FXML
	public ImageView webcamImageView;

	@FXML
	private Label stepEncodageCnous;

	@FXML
	private Label stepSendCSV;

	@FXML
	public ImageView bmpBlackImageView;

	@FXML
	public ImageView bmpColorImageView;

	private String lastText = null;

	public ObjectProperty<Image> imageProperty;
	
	public Webcam webcam = null;

	private BufferedImage webcamBufferedImage;

	public static SimpleBooleanProperty webcamReady = new SimpleBooleanProperty(false);

	public SimpleBooleanProperty nfcReady = new SimpleBooleanProperty();

	public static SimpleBooleanProperty authReady = new SimpleBooleanProperty();

	EsupSgcGetBmpTaskService esupSgcGetBmpColorTaskService;

	EsupSgcGetBmpTaskService esupSgcGetBmpBlackTaskService;

	QrCodeTaskService qrCodeTaskService;

	EncodingTaskService encodeTaskService;

	EvolisPrintTaskService evolisPrintTaskService;

	EvolisEjectTaskService evolisEjectTaskService;

	EsupSgcLongPollTaskService esupSgcLongPollTaskService;

	WaitRemoveCardTaskService waitRemoveCardTaskService;

	public void init(String esupNfcTagServerUrl) {

		nfcTagPane.getChildren().add(new EsupNfcClientStackPane(esupNfcTagServerUrl, Utils.getMacAddress()));

		comboBox.getSelectionModel().selectedItemProperty().addListener((options, oldWebcamName, newWebcamName) -> {
			log.debug("comboBox SelectionModel Event : " + options.getValue() + " - " +  oldWebcamName + " - " + newWebcamName);
			if(options.getValue()!=null && newWebcamName!=null && !newWebcamName.equals(oldWebcamName) && (oldWebcamName!=null || webcam==null)) {
					 Thread t = new Thread(() -> {
						synchronized (newWebcamName) {
							Webcam newWebcam = Webcam.getWebcamByName(newWebcamName);
								if (webcam != null && webcam.isOpen()) {
									webcam.close();
								}
								if (!newWebcam.isOpen()) {
									try {
										newWebcam.open();
									} catch(Exception e) {
										log.warn("can't start camera", e);
									}
								}
								if (!newWebcam.getLock().isLocked()) {
									newWebcam.getLock().unlock();
								}
								webcam = newWebcam;
								checkCamera.getTooltip().setText(webcam.getName());
								webcamImageView.imageProperty().unbind();
								threadWebcamStream.interrupt();
								webcamReady.set(webcam.isOpen());
							}
					});
					 t.start();
				try {
					t.join(1000);
				} catch (InterruptedException e) {
					log.error(e);
				}
				if (t.isAlive()) {
					log.warn("can't start webcam " + newWebcamName);
					t.interrupt();
				}
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
					addLogTextLn("INFO", "PC/SC OK");
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
					addLogTextLn("INFO", "Authentification OK : " + FileLocalStorage.eppnInit);
					startLoopServiceIfPossible();
				} else {
					checkAuth.getStyleClass().clear();
					checkAuth.getStyleClass().add("btn-danger");
					checkAuth.getTooltip().setText("...");
				}
			}
		});

		EvolisHeartbeatTask.printerReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkPrinter.getStyleClass().clear();
					checkPrinter.getStyleClass().add("btn-success");
					addLogTextLn("INFO", "imprimante evolis OK");
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
				if(threadWebcamStream!=null && threadWebcamStream.isAlive()) {
					threadWebcamStream.interrupt();
				}
				if(newValue) {
							log.info("restart with webcam " + webcam);
							checkCamera.getStyleClass().clear();
							checkCamera.getStyleClass().add("btn-success");
							webcamImageView.setVisible(true);
							webcamImageView.setManaged(true);
							// bmpBlackImageView.setVisible(false);
							// bmpBlackImageView.setManaged(false);
							// bmpColorImageView.setVisible(false);
							// bmpColorImageView.setManaged(false);
							checkCamera.getTooltip().setText(webcam.getName());
							startWebCamStream();
							// primaryStage.sizeToScene();
							addLogTextLn("INFO", "Caméra OK : " + webcam);
							startLoopServiceIfPossible();
				} else {
							checkCamera.getStyleClass().clear();
							checkCamera.getStyleClass().add("btn-danger");
							addLogTextLn("ERROR", "Caméra déconnectée ?!");
				}
				Platform.runLater(new Runnable() {
					@Override
					public void run() {
						comboBox.setItems(FXCollections.observableList(new ArrayList<String>()));
						for (Webcam webcam : Webcam.getWebcams()) {
							if(!comboBox.getItems().contains(webcam.getName())) {
								comboBox.getItems().add(webcam.getName());
							}
						}
						if (webcam != null) {
							comboBox.getSelectionModel().select(webcam.getName());
						}
					}});
			}
		});

		qrCodeTaskService = new QrCodeTaskService(webcamImageView.imageProperty());
		qrCodeTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
            @Override
            public void handle(WorkerStateEvent t) {
                String qrcode = qrCodeTaskService.getValue();
				encodeTaskService = new EncodingTaskService(qrcode);
				encodeTaskService.setOnFailed(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent t) {
						waitRemoveCardTaskService.restart();
					}
				});
				encodeTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent t) {
						waitRemoveCardTaskService.restart();
					}
				});
				encodeTaskService.start();
            }
        });

		waitRemoveCardTaskService = new WaitRemoveCardTaskService();
		waitRemoveCardTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				log.info("restart qrcode task ...");
				qrCodeTaskService.restart();
			}
		});

		esupSgcLongPollTaskService = new EsupSgcLongPollTaskService();
		esupSgcLongPollTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				String qrcode = esupSgcLongPollTaskService.getValue();
				esupSgcGetBmpColorTaskService = new EsupSgcGetBmpTaskService(qrcode, EncodingService.BmpType.color, bmpColorImageView);
				esupSgcGetBmpColorTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
					@Override
					public void handle(WorkerStateEvent t) {
						String qrcode = esupSgcLongPollTaskService.getValue();
						esupSgcGetBmpBlackTaskService = new EsupSgcGetBmpTaskService(qrcode, EncodingService.BmpType.black, bmpBlackImageView);
						esupSgcGetBmpBlackTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
							@Override
							public void handle(WorkerStateEvent t) {
								String bmpColorImageView = esupSgcGetBmpColorTaskService.getValue();
								String bmpBlackImageView = esupSgcGetBmpBlackTaskService.getValue();
								evolisPrintTaskService = new EvolisPrintTaskService(bmpColorImageView, bmpBlackImageView);
								evolisPrintTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
									@Override
									public void handle(WorkerStateEvent t) {
										encodeTaskService = new EncodingTaskService(qrcode);
										encodeTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
											@Override
											public void handle(WorkerStateEvent t) {
												evolisEjectTaskService = new EvolisEjectTaskService(true);
												evolisEjectTaskService.start();
												evolisEjectTaskService.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
													@Override
													public void handle(WorkerStateEvent t) {
														esupSgcLongPollTaskService.restart();
													}
												});
											}
										});
										encodeTaskService.start();
									}
								});
								evolisPrintTaskService.start();
							}
						});
						esupSgcGetBmpBlackTaskService.start();
					}
				});
				esupSgcGetBmpColorTaskService.start();
			}
		});
		esupSgcLongPollTaskService.start();
		Webcam.addDiscoveryListener(new EsupWebcamDiscoveryListener(this));

		comboBox.setItems(FXCollections.observableList(new ArrayList<String>()));
		for(Webcam webcam : Webcam.getWebcams()) {
			if(!comboBox.getItems().contains(webcam.getName())) {
				comboBox.getItems().add(webcam.getName());
			}
		}

		webcam = Webcam.getDefault();
		if(webcam!=null) {
			comboBox.getSelectionModel().select(webcam.getName());
		}

	}

	private void startLoopServiceIfPossible() {
		if(authReady.getValue() && nfcReady.getValue() && webcamReady.getValue() && !qrCodeTaskService.isRunning()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					qrCodeTaskService.start();
				}
			});
		}
		if(authReady.getValue() && nfcReady.getValue() && EvolisHeartbeatTask.printerReady.getValue() && !esupSgcLongPollTaskService.isRunning()) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					esupSgcLongPollTaskService.start();
				}
			});
		}
	}

	private void startWebCamStream() {
		log.debug("startWebCamStream ...");
		imageProperty = new SimpleObjectProperty<Image>();
		Task<Void> webcamUiTask = new WebcamUiTask(webcam, imageProperty);
		threadWebcamStream = new Thread(webcamUiTask);
		threadWebcamStream.setDaemon(true);
		threadWebcamStream.start();
		webcamImageView.imageProperty().bind(imageProperty);
		webcamImageView.setRotate(180);
	}

	public void changeTextPrincipal(String text, StyleLevel styleLevel) {
		textPrincipal.setText(text);
		panePrincipal.getStyleClass().clear();
		panePrincipal.getStyleClass().add("panel-" + styleLevel);
	}

	public void changeStepClientReady(String text, StyleLevel styleLevel) {
		stepClientReady.setText(text);
		stepClientReady.getParent().getStyleClass().clear();
		stepClientReady.getParent().getStyleClass().add("alert-" + styleLevel);
	}

	public void changeStepReadQR(StyleLevel styleLevel) {
		stepReadQR.getParent().getStyleClass().clear();
		stepReadQR.getParent().getStyleClass().add("alert-" + styleLevel);
	}

	public void changeStepReadCSN(StyleLevel styleLevel) {
		stepReadCSN.getParent().getStyleClass().clear();
		stepReadCSN.getParent().getStyleClass().add("alert-" + styleLevel);
	}

	public void changeStepSelectSGC(StyleLevel styleLevel) {
		stepSelectSGC.getParent().getStyleClass().clear();
		stepSelectSGC.getParent().getStyleClass().add("alert-" + styleLevel);
	}

	public void changeStepEncodageApp(StyleLevel styleLevel) {
		stepEncodageApp.getParent().getStyleClass().clear();
		stepEncodageApp.getParent().getStyleClass().add("alert-" + styleLevel);
	}

	public void changeStepEncodageCnous(StyleLevel styleLevel) {
		stepEncodageCnous.getParent().getStyleClass().clear();
		stepEncodageCnous.getParent().getStyleClass().add("alert-" + styleLevel);
	}

	public void changeStepSendCSV(StyleLevel styleLevel) {
		stepSendCSV.getParent().getStyleClass().clear();
		stepSendCSV.getParent().getStyleClass().add("alert-" + styleLevel);
	}

	public void hideCnousSteps() {
		stepEncodageCnous.setVisible(false);
		stepSendCSV.setVisible(false);
	}

	public void setOk() {
		//changeStepClientReady("Client prêt", MainController.StyleLevel.success);
		//changeTextPrincipal("En attente d'une carte...", MainController.StyleLevel.success);
		log.info("client ok");
	}
	
	public void addLogTextLn(String type, String text) {
		if (!text.equals(lastText)) {
			Platform.runLater(new Runnable() {
				@Override
				public void run() {
					String date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
					logTextarea.appendText("[" + type + "] " + date + " - " + text + "\n");
					lastText = text;
					logTextarea.positionCaret(logTextarea.getLength());
				}});
		}
	}

	public void exit() {
		if (webcam != null) {
			webcam.close();
		}
	}


}
