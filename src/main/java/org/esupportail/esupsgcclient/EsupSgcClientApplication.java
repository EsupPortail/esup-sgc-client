package org.esupportail.esupsgcclient;

import java.io.IOException;
import java.net.URL;
import java.util.Properties;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.pcsc.InitEncodingServiceTask;
import org.esupportail.esupsgcclient.ui.FileLocalStorage;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;

public class EsupSgcClientApplication extends Application {

	private final static Logger log = Logger.getLogger(EsupSgcClientApplication.class);
	public static EsupSgcClientJfxController mainPane;
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

		URL fxmlUrl = this.getClass().getClassLoader().getResource("esup-sgc-client.fxml");
		FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
		VBox root = fxmlLoader.load();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();

		mainPane = fxmlLoader.getController();
		mainPane.primaryStage = primaryStage;
		mainPane.init(esupNfcTagServerUrl);

		FileLocalStorage.setAuthReady(mainPane.authReady);
		InitEncodingServiceTask clientCheckService = new InitEncodingServiceTask(mainPane);
		Thread clientCheckThread = new Thread(clientCheckService);
		clientCheckThread.setDaemon(true);
		clientCheckThread.start();
		
	}
    
}
