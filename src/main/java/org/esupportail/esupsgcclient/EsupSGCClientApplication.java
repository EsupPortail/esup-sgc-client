package org.esupportail.esupsgcclient;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.net.URL;
import java.util.Enumeration;
import java.util.Properties;

import javafx.beans.property.SimpleBooleanProperty;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Insets;
import javafx.scene.layout.Background;
import javafx.scene.layout.BackgroundFill;
import javafx.scene.layout.CornerRadii;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.ClientCheckService;
import org.esupportail.esupsgcclient.service.MainLoopService;
import org.esupportail.esupsgcclient.task.WaitClientReadyTask;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.ui.FileLocalStorage;
import org.esupportail.esupsgcclient.ui.MainController;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EsupSGCClientApplication extends Application {

	private final static Logger log = Logger.getLogger(EsupSGCClientApplication.class);

	public static MainController mainPane;
	private static MainLoopService mainService;
	public static String esupNfcTagServerUrl;
	public static String esupSgcUrl;
	public static boolean encodeCnous;
	public static String numeroId;
	public static String sgcAuthToken;

	public void start(final Stage primaryStage) throws IOException {

		Properties prop = new Properties();
		Resource resource = new ClassPathResource("esupsgcclient.properties");
		try {
			prop.load(resource.getInputStream());
			log.info("load props");
		} catch (IOException e1) {
			log.error("props not found");
		} 
		esupNfcTagServerUrl = System.getProperty("esupNfcTagServerUrl", prop.getProperty("esupNfcTagServerUrl"));
		esupSgcUrl = System.getProperty("esupSgcUrl", prop.getProperty("esupSgcUrl"));
		encodeCnous = Boolean.valueOf(System.getProperty("encodeCnous", prop.getProperty("encodeCnous")));
		
		primaryStage.setTitle("Esup-SGC-Client");

		primaryStage.setOnCloseRequest(event -> {
			mainPane.exit();
			System.exit(0);
		});

		URL fxmlUrl = this.getClass().getClassLoader().getResource("esup-sgc-client.fxml");
		FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
		mainPane = fxmlLoader.getController();
		VBox root = fxmlLoader.load();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();

		mainPane = fxmlLoader.getController();

		mainPane.init();
		mainPane.changeTextPrincipal("Chargement...", MainController.StyleLevel.warning);

		primaryStage.setOnCloseRequest(we -> stop());
		
		mainPane.nfcTagPane.getChildren().add(new EsupNfcClientStackPane(esupNfcTagServerUrl, getMacAddress()));

		mainService = new MainLoopService(mainPane);

		FileLocalStorage.setAuthReady(mainPane.authReady);

		mainPane.nfcReady.addListener(new ChangeListener<Boolean>() {
			@Override
			public void changed(ObservableValue<? extends Boolean> observableValue, Boolean oldValue, Boolean newValue) {
				if(newValue) {
					mainPane.setOk();
					mainService.start();
				}
			}
		});

		ClientCheckService clientCheckService = new ClientCheckService(mainPane);
		Thread clientCheckThread = new Thread(clientCheckService);
		clientCheckThread.setDaemon(true);
		clientCheckThread.start();
		
	}
	
	@Override
	public void stop(){
		mainPane.exit();
		System.exit(0);
	}

    private static String getMacAddress() {
    	Enumeration<NetworkInterface> netInts = null;
    	try {
			netInts = NetworkInterface.getNetworkInterfaces();
		} catch (SocketException e1) {
			log.error("error get network int list");
		}
    	final StringBuilder sb = new StringBuilder();
		while(true) {
			byte[] mac = null;
			try {
				NetworkInterface netInf = netInts.nextElement();
				mac = netInf.getHardwareAddress();
				if(mac != null) {
					if(mac.length>0) {
				    	for (int i = 0; i < mac.length; i++) {
				    	        sb.append(String.format("%02X%s", mac[i], (i < mac.length - 1) ? ":" : ""));
				    	}	
			    		break;
					}
		    	}
			} catch (Exception e) {
				log.error("mac address read error");
			}

		}
		return sb.toString();
	}
    
}
