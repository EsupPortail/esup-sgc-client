package org.esupportail.esupsgcclient.ui;

import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.collections.FXCollections;
import javafx.concurrent.WorkerStateEvent;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.FlowPane;
import javafx.scene.layout.Region;
import javafx.scene.paint.Color;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.InitEvolisServiceTask;
import org.esupportail.esupsgcclient.service.webcam.EsupWebcamDiscoveryListener;
import org.esupportail.esupsgcclient.task.WebcamUiTask;
import org.esupportail.esupsgcclient.utils.Utils;

import com.github.sarxos.webcam.Webcam;

import javafx.beans.property.ObjectProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.property.SimpleObjectProperty;
import javafx.concurrent.Task;
import javafx.embed.swing.SwingFXUtils;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.Pane;

public class MainController {

	final static Logger log = Logger.getLogger(MainController.class);

	public enum StyleLevel {success, danger, warning, info};

	private Thread threadWebcamStream = null;

	@FXML
	private Button buttonLogs;

	@FXML
	private Button buttonNfcTag;

	@FXML
	private Region checkAuth;

	@FXML
	private Region checkCamera;

	@FXML
	private Region checkNfc;

	@FXML
	private Region checkPrinter;

	@FXML
	private ComboBox<Webcam> comboBox;

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
	
	public ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>();
	
	public Webcam webcam = null;

	private BufferedImage webcamBufferedImage;

	public SimpleBooleanProperty webcamReady = new SimpleBooleanProperty(false);

	public SimpleBooleanProperty nfcReady = new SimpleBooleanProperty();

	public static SimpleBooleanProperty authReady = new SimpleBooleanProperty();

	public void init() {
		
		comboBox.setOnAction((event) -> {
		    Webcam newWebcam = (Webcam) comboBox.getSelectionModel().getSelectedItem();
			if(newWebcam!=null) {
				newWebcam.open();
				if (newWebcam.getImage() != null && webcam != newWebcam) {
					webcam.close();
					webcam = newWebcam;
					threadWebcamStream.interrupt();
					startWebCamStream();
				} else {
					comboBox.getSelectionModel().select(webcam);
				}
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
			}
		});


		nfcReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkNfc.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
				} else {
					checkNfc.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
				}
			}
		});

		authReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					checkAuth.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
				} else {
					checkAuth.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
				}
			}
		});

		InitEvolisServiceTask.printerReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					changeStepClientReady("Ouverture de la webcam", MainController.StyleLevel.warning);
					addLogTextLn("INFO", "webcam OK : " + webcam);
					checkPrinter.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
				} else {
					checkPrinter.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
				}
			}
		});


		webcamReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							log.debug("start webcam ...");
							startWebCamStream();
							checkCamera.setBackground(new Background(new BackgroundFill(Color.GREEN, CornerRadii.EMPTY, Insets.EMPTY)));
						}
					});
				} else {
					Platform.runLater(new Runnable() {
						@Override
						public void run() {
							checkCamera.setBackground(new Background(new BackgroundFill(Color.RED, CornerRadii.EMPTY, Insets.EMPTY)));
						}
					});
				}
			}
		});

		Webcam.addDiscoveryListener(new EsupWebcamDiscoveryListener(this));

		comboBox.setItems(FXCollections.observableList(new ArrayList<Webcam>()));
		for(Webcam webcam : Webcam.getWebcams()) {
			comboBox.getItems().add(webcam);
		}

		webcam = Webcam.getDefault();
		comboBox.getSelectionModel().select(webcam);

	}

	private void startWebCamStream() {
		Task<Void> task = new WebcamUiTask(webcam, webcamBufferedImage, imageProperty);
		threadWebcamStream = new Thread(task);
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
		changeStepClientReady("Client prêt", MainController.StyleLevel.success);
		changeTextPrincipal("En attente d'une carte...", MainController.StyleLevel.success);
	}
	
	public void addLogTextLn(String type, String text) {
		if (!text.equals(lastText)) {
			String date = new SimpleDateFormat("dd/MM/yyyy HH:mm:ss").format(new Date());
			logTextarea.appendText("[" + type + "] " + date + " - " + text + "\n");
			lastText = text;
			logTextarea.positionCaret(logTextarea.getLength());
		}
	}

	public void exit() {
		if (webcam != null) {
			webcam.close();
		}
	}


}
