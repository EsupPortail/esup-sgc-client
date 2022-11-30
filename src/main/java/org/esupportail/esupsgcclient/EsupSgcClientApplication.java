package org.esupportail.esupsgcclient;

import java.io.IOException;
import java.net.URL;

import javafx.fxml.FXMLLoader;
import javafx.scene.layout.VBox;
import org.apache.log4j.Logger;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;

import javafx.application.Application;
import javafx.scene.Scene;
import javafx.stage.Stage;
import org.springframework.context.annotation.ComponentScan;

@ComponentScan
public class EsupSgcClientApplication extends Application {

	private final static Logger log = Logger.getLogger(EsupSgcClientApplication.class);

	Stage pStage;

	public void start(final Stage primaryStage) throws IOException {

		ApplicationContext context = new AnnotationConfigApplicationContext(EsupSgcClientApplication.class);
		
		primaryStage.setTitle("Esup-SGC-Client");

		URL fxmlUrl = this.getClass().getClassLoader().getResource("esup-sgc-client.fxml");
		FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
		fxmlLoader.setControllerFactory(cls -> context.getBean(cls));
		VBox root = fxmlLoader.load();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);
		primaryStage.show();
		this.pStage = primaryStage;
	}

	public Stage getPrimaryStage() {
		return pStage;
	}
    
}
