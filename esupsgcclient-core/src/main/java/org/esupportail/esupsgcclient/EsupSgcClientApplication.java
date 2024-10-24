package org.esupportail.esupsgcclient;

import javafx.application.Application;
import javafx.application.Platform;
import javafx.event.EventHandler;
import javafx.fxml.FXMLLoader;
import javafx.geometry.Rectangle2D;
import javafx.scene.Scene;
import javafx.scene.layout.VBox;
import javafx.stage.Screen;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.AnnotationConfigApplicationContext;
import org.springframework.context.annotation.ComponentScan;

import java.io.IOException;
import java.net.URL;

@ComponentScan
public class EsupSgcClientApplication extends Application {

	private final static Logger log = Logger.getLogger(EsupSgcClientApplication.class);

	public void start(final Stage primaryStage) throws IOException {

		long start = System.currentTimeMillis();

		ApplicationContext context = new AnnotationConfigApplicationContext(EsupSgcClientApplication.class);
		
		primaryStage.setTitle("Esup-SGC-Client");

		URL fxmlUrl = this.getClass().getClassLoader().getResource("esup-sgc-client.fxml");
		FXMLLoader fxmlLoader = new FXMLLoader(fxmlUrl);
		fxmlLoader.setControllerFactory(cls -> context.getBean(cls));
		VBox root = fxmlLoader.load();
		Scene scene = new Scene(root);
		primaryStage.setScene(scene);

		EsupSgcClientJfxController esupSgcClientJfxController = fxmlLoader.getController();
		esupSgcClientJfxController.initializeFromFileLocalStorage(primaryStage);

		primaryStage.show();

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			@Override
			public void handle(WindowEvent e) {
				esupSgcClientJfxController.exit();
			}
		});
		esupSgcClientJfxController.logTextAreaService.appendText(String.format("Application initialized in %.2f seconds", (System.currentTimeMillis()-start)/1000.0));
	}

}
