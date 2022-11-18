package org.esupportail.esupsgcclient.ui;

import java.awt.Dimension;
import java.awt.Graphics2D;
import java.awt.image.BufferedImage;
import java.io.IOException;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicReference;

import javax.imageio.ImageIO;

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
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.Button;
import javafx.scene.control.ComboBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.image.WritableImage;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.Pane;
import javafx.scene.layout.VBox;

public class MainPane extends Pane {

	private final static Logger log = Logger.getLogger(MainPane.class);

	private Thread threadWebcamStream = null;
	
	private static int fontSize = 24;
	private static int centerPaneHeight = 250;
	private static int padding = 10;

	private ImageView image1JPanel = new ImageView();
	private ImageView image2JPanel = new ImageView();

	private Label stepClientReady = new Label("Client en cours de chargement");
	private Label stepReadQR = new Label("Lecture du QRCode");
	private Label stepReadCSN = new Label("Lecture du CSN");
	private Label stepSelectSGC = new Label("Selection dans le SGC");
	private Label stepEncodageApp = new Label("Encodage de la carte");
	private Label stepEncodageCnous = new Label("Encodage CNOUS");
	private Label stepSendCSV = new Label("Envoi du CSV");

	public ComboBox<Webcam> comboBox = new ComboBox<Webcam>();
	
	public BorderPane webCamPane = new BorderPane();
	public ImageView webcamImageView;

	public ImageView bmpBlackImageView = new ImageView();;

	public ImageView bmpColorImageView = new ImageView();;
	
	private Button buttonLogs = new Button("Masquer les logs");
	private Pane logPane = new Pane();
	private int nfcTagSize = 500;
	private Button buttonNfcTag = new Button("Masquer EsupNfcTag");
	public Pane nfcTagPane = new Pane();
	
	public Button buttonExit = new Button("Quitter");
	public Button buttonRestart = new Button("Restart");

	private String lastText = null;
	private Label textPrincipal = new Label("");
	
	public ObjectProperty<Image> imageProperty = new SimpleObjectProperty<Image>();
	
	public TextArea logTextarea = new TextArea();
	
	public Webcam webcam = null;	
	private BufferedImage webcamBufferedImage;

	public SimpleBooleanProperty webCamReady = new SimpleBooleanProperty(false);

	public MainPane(int width, int height) {
		
		
		BorderPane logosPane = new BorderPane();
		logosPane.setStyle("-fx-background-color: #e0e0e0");
		logosPane.setPadding(new Insets(padding, padding, 0, padding));
		logosPane.setMaxSize(width - nfcTagSize - padding * 2, 100);
		logosPane.setLeft(image1JPanel);
		logosPane.setRight(image2JPanel);

		Label title = new Label("");
		title.setStyle("-fx-font-size: 32;");
		title.setText("Status : ");
		title.setMinSize(200, 50);

		textPrincipal.setMinSize(500, 50);
		textPrincipal.setMaxSize(500, 50);

		HBox titlePane = new HBox();
		titlePane.setPadding(new Insets(0, 0, 0, padding));
		titlePane.getChildren().add(title);
		titlePane.getChildren().add(textPrincipal);
		
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
		
		VBox processPane = new VBox();
		processPane.setMinSize(500, centerPaneHeight);
		processPane.setPadding(new Insets(0, 0, 0, padding));
		processPane.getChildren().add(stepClientReady);
		processPane.getChildren().add(stepReadQR);
		processPane.getChildren().add(stepReadCSN);
		processPane.getChildren().add(stepSelectSGC);
		processPane.getChildren().add(stepEncodageApp);
		processPane.getChildren().add(stepEncodageCnous);
		processPane.getChildren().add(stepSendCSV);

		webCamPane.setMinSize(400, centerPaneHeight);
		webCamPane.setMaxSize(500, centerPaneHeight);
		webcamImageView = new ImageView();
		webCamPane.setCenter(webcamImageView);
		setImageViewSize();

		BorderPane centerPane = new BorderPane();
		centerPane.setMinSize(width - nfcTagSize, centerPaneHeight);
		centerPane.setLeft(processPane);
		centerPane.setRight(webCamPane);

		HBox buttonsPane = new HBox();
		buttonsPane.setAlignment(Pos.BASELINE_RIGHT);
		buttonsPane.setMaxSize(width - nfcTagSize - padding * 2, 50);
		buttonsPane.setPadding(new Insets(0, padding, 0, 0));
		buttonsPane.setSpacing(10);
		buttonsPane.getChildren().add(buttonRestart);
		buttonsPane.getChildren().add(buttonNfcTag);
		buttonsPane.getChildren().add(buttonLogs);

		logPane.setMaxSize(width - nfcTagSize , 250);
		logTextarea.setEditable(false);
		logTextarea.setMinSize(width - nfcTagSize, 230);
		logPane.getChildren().add(logTextarea);
		buttonLogs.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				if (logPane.isVisible()) {
					logPane.setVisible(false);
					buttonLogs.setText("Afficher les logs");
				} else {
					logPane.setVisible(true);
					buttonLogs.setText("Masquer les logs");
				}
			}
		});

		nfcTagPane.setMaxSize(nfcTagSize , height);
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
		
		VBox mainPane = new VBox();
		mainPane.setStyle("-fx-background-color: #e0e0e0");
		mainPane.setMinSize(width - nfcTagSize, height);
		mainPane.setSpacing(10);
		mainPane.getChildren().add(logosPane);
		mainPane.getChildren().add(comboBox);		
		mainPane.getChildren().add(titlePane);
		mainPane.getChildren().add(centerPane);
		mainPane.getChildren().add(logPane);
		mainPane.getChildren().add(buttonsPane);

		HBox allPane = new HBox();
		allPane.setStyle("-fx-background-color: #e0e0e0");
		allPane.setMinSize(width, height);
		allPane.getChildren().add(nfcTagPane);
		allPane.getChildren().add(mainPane);
		
		getChildren().add(allPane);

		initializeWebCam();

	}

	public BufferedImage getWebcamBufferedImage() {
		return webcamBufferedImage;
	}

	public void setImageViewSize() {
		double height = centerPaneHeight;
		webcamImageView.maxHeight(height);
		webcamImageView.setFitHeight(height);
		webcamImageView.setPreserveRatio(true);
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
							setImageViewSize();
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

	public void setLogo1(URL url) {
		BufferedImage bufImg1;
		try {
			bufImg1 = ImageIO.read(url);
			image1JPanel.setImage(SwingFXUtils.toFXImage(bufImg1, null));
		} catch (IOException e) {
			log.error("logo 1 loading error");
		}
	}

	public void setLogo2(URL url) {
		BufferedImage bufImg1;
		try {
			bufImg1 = ImageIO.read(url);
			image2JPanel.setImage(SwingFXUtils.toFXImage(bufImg1, null));
		} catch (IOException e) {
			log.error("logo 2 loading error");
		}
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
