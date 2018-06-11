package org.esupportail.esupsgcclient;

import org.esupportail.esupsgcclient.security.EsupSecurityManager;
import org.esupportail.esupsgcclient.service.ClientCheckService;
import org.esupportail.esupsgcclient.service.MainLoopService;
import org.esupportail.esupsgcclient.task.WaitClientReadyTask;
import org.esupportail.esupsgcclient.ui.MainPane;

import javafx.application.Application;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

@SuppressWarnings("restriction")
public class EsupSGCClientApplication extends Application {

	private static String[] arguments;
	private static int width = 1000;
	private static int height = 750;
	
	public static void main(String... args) throws Exception {
		System.setSecurityManager(new EsupSecurityManager());
		arguments = args;
		launch(args);
	}

	public void start(final Stage primaryStage) {

		MainPane mainPane = new MainPane(width, height);

		MainLoopService mainService = new MainLoopService(mainPane);	
		
		primaryStage.setTitle("Esup-SGC-Client");
		primaryStage.setMinWidth(width);
		primaryStage.setMinHeight(height + 20);

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent event) {
				mainPane.exit();
				System.exit(0);
			}
		});

		Group root = new Group();
		root.getChildren().add(mainPane);
		Color backgroundColor = Color.web("#e0e0e0");
		final Scene scene = new Scene(root, width, height, backgroundColor);
		primaryStage.setScene(scene);
		primaryStage.show();

		mainPane.initUi();
		mainPane.changeTextPrincipal("Chargement...", "orange");
		mainPane.buttonRestart.setVisible(false);
		mainPane.buttonExit.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				mainPane.exit();
				System.exit(0);
			}
		});

		ClientCheckService clientCheckService = new ClientCheckService(mainPane);
		clientCheckService.setArgs(arguments);
		clientCheckService.start();

		WaitClientReadyTask waitClientReadyTask = new WaitClientReadyTask();
		waitClientReadyTask.clientReady.bind(clientCheckService.clientReady);
		waitClientReadyTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				mainService.start();
			}
		});
		clientCheckService.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event) {
				Thread waitClientReadyThread = new Thread(waitClientReadyTask);
				waitClientReadyThread.setDaemon(true);
				waitClientReadyThread.start();			  
			}
		});
	}

}