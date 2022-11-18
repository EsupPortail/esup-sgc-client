package org.esupportail.esupsgcclient.ui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.utils.Utils;

import com.github.sarxos.webcam.Webcam;
import com.github.sarxos.webcam.WebcamImageTransformer;
import com.github.sarxos.webcam.WebcamResolution;

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

	private final static Logger log = Logger.getLogger(MainController.class);

	private static int fontSize = 24;

	private Thread threadWebcamStream = null;

	@FXML
	private Button buttonLogs;

	@FXML
	private Button buttonNfcTag;

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
	private Label textPrincipal;

	@FXML
	private Label title;

	@FXML
	private ImageView webcamImageView;

	@FXML
	private Label stepEncodageCnous;

	@FXML
	private Label stepSendCSV;

	public ImageView bmpBlackImageView = new ImageView();;

	public ImageView bmpColorImageView = new ImageView();;
	
	public Button buttonExit = new Button("Quitter");
	public Button buttonRestart = new Button("Restart");

	private String lastText = null;
	
	public ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>();
	
	public Webcam webcam = null;	
	private BufferedImage webcamBufferedImage;

	public SimpleBooleanProperty webCamReady = new SimpleBooleanProperty(false);

	public void init() {

		comboBox.setItems(FXCollections.observableList(new ArrayList<Webcam>()));
		for(Webcam webcam : Webcam.getWebcams()) {
			comboBox.getItems().add(webcam);
		}
		
		comboBox.setOnAction((event) -> {
			webcam.close();
		    webcam = (Webcam) comboBox.getSelectionModel().getSelectedItem();
		    threadWebcamStream.interrupt();
		    initializeWebCam();
		    startWebCamStream();
		    
		});
		
		comboBox.getSelectionModel().select(Webcam.getDefault());
		webcam = Webcam.getDefault();

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

		initializeWebCam();

	}

	public BufferedImage getWebcamBufferedImage() {
		return webcamBufferedImage;
	}

	private void initializeWebCam() {
		Task<Void> webCamTask = new Task<Void>() {

			@Override
			protected Void call() throws Exception {
				Dimension[] nonStandardResolutions = new Dimension[] { WebcamResolution.PAL.getSize(),
						WebcamResolution.HD.getSize(), new Dimension(720, 480), new Dimension(1920, 1080), };

				Dimension size = WebcamResolution.VGA.getSize();
				if (webcam != null) {
					webcam.close();
					webcam.setCustomViewSizes(nonStandardResolutions);
					webcam.setViewSize(size);
					webcam.setImageTransformer(new WebcamImageTransformer() {
						public BufferedImage transform(BufferedImage image) {
							BufferedImage bi = new BufferedImage(image.getWidth(), image.getHeight(),
									BufferedImage.TYPE_INT_BGR);
							Graphics2D g2 = bi.createGraphics();
							g2.rotate(Math.PI, image.getWidth() / 2.0, image.getHeight() / 2.0);
							g2.drawImage(image, 0, 0, null);
							g2.dispose();
							bi.flush();
							return bi;
						}
					});

					webcam.open();
					webCamReady.set(true);
				}
				startWebCamStream();
				return null;
			}
		};
		Thread webCamThread = new Thread(webCamTask);
		webCamThread.setDaemon(true);
		webCamThread.start();
	}

	private void startWebCamStream() {

		Task<Void> task = new Task<Void>() {

			@Override
			protected Void call() throws Exception {

				final AtomicReference<WritableImage> ref = new AtomicReference<>();
				while (true) {
					try {
						if (webcam != null && (webcamBufferedImage = webcam.getImage()) != null) {
							ref.set(SwingFXUtils.toFXImage(webcamBufferedImage, ref.get()));
							webcamBufferedImage.flush();
							imageProperty.set(ref.get());
						}
					} catch (Exception e) {
						e.printStackTrace();
					}
					Utils.sleep(250);
				}
			}
		};

		threadWebcamStream = new Thread(task);
		threadWebcamStream.setDaemon(true);
		threadWebcamStream.start();
		webcamImageView.imageProperty().bind(imageProperty);

	}

	public void changeTextPrincipal(String text, String color) {
		textPrincipal.setText(text);
		textPrincipal.setStyle("-fx-text-fill: " + color + "; -fx-font-size: 32;");
	}

	public void changeStepClientReady(String text, String color) {
		stepClientReady.setText(text);
		stepClientReady.setStyle("-fx-text-fill: " + color + ";-fx-font-size: " + fontSize + ";");
	}

	public void changeStepReadQR(String color) {
		stepReadQR.setStyle("-fx-text-fill: " + color + ";-fx-font-size: " + fontSize + ";");
	}

	public void changeStepReadCSN(String color) {
		stepReadCSN.setStyle("-fx-text-fill: " + color + ";-fx-font-size: " + fontSize + ";");
	}

	public void changeStepSelectSGC(String color) {
		stepSelectSGC.setStyle("-fx-text-fill: " + color + ";-fx-font-size: " + fontSize + ";");
	}

	public void changeStepEncodageApp(String color) {
		stepEncodageApp.setStyle("-fx-text-fill: " + color + ";-fx-font-size: " + fontSize + ";");
	}

	public void changeStepEncodageCnous(String color) {
		stepEncodageCnous.setStyle("-fx-text-fill: " + color + ";-fx-font-size: " + fontSize + ";");
	}

	public void changeStepSendCSV(String color) {
		stepSendCSV.setStyle("-fx-text-fill: " + color + ";-fx-font-size: " + fontSize + ";");
	}

	public void hideCnousSteps() {
		stepEncodageCnous.setVisible(false);
		stepSendCSV.setVisible(false);
	}

	public void initUi() {
		stepClientReady.setStyle("-fx-text-fill: gray;-fx-font-size: " + fontSize + ";");
		stepReadQR.setStyle("-fx-text-fill: gray;-fx-font-size: " + fontSize + ";");
		stepReadCSN.setStyle("-fx-text-fill: gray;-fx-font-size: " + fontSize + ";");
		stepSelectSGC.setStyle("-fx-text-fill: gray;-fx-font-size: " + fontSize + ";");
		stepEncodageApp.setStyle("-fx-text-fill: gray;-fx-font-size: " + fontSize + ";");
		stepEncodageCnous.setStyle("-fx-text-fill: gray;-fx-font-size: " + fontSize + ";");
		stepSendCSV.setStyle("-fx-text-fill: gray;-fx-font-size: " + fontSize + ";");
	}

	public void setOk() {
		changeStepClientReady("Client prêt", "green");
		changeTextPrincipal("En attente d'une carte...", "green");
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
