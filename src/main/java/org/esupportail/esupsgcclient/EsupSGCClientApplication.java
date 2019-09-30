package org.esupportail.esupsgcclient;

import java.io.IOException;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.ClientCheckService;
import org.esupportail.esupsgcclient.service.MainLoopService;
import org.esupportail.esupsgcclient.task.WaitClientReadyTask;
import org.esupportail.esupsgcclient.ui.EsupNfcClientStackPane;
import org.esupportail.esupsgcclient.ui.FileLocalStorage;
import org.esupportail.esupsgcclient.ui.MainPane;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

import javafx.application.Application;
import javafx.concurrent.Task;
import javafx.concurrent.WorkerStateEvent;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.paint.Color;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;
import netscape.javascript.JSObject;

public class EsupSGCClientApplication extends Application {

	private final static Logger log = Logger.getLogger(EsupSGCClientApplication.class);

	private static Color backgroundColor = Color.web("#e0e0e0");
	private static int width = 1500;
	private static int height = 750;
	public static MainPane mainPane = new MainPane(width, height);
	private static MainLoopService mainService = new MainLoopService(mainPane);
	public static String esupNfcTagServerUrl;
	public static String esupSgcUrl;
	public static boolean encodeCnous;
	public static String numeroId;
	public static String eppnInit;
	public static JSObject window;
	

	public void start(final Stage primaryStage) {
		
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
		encodeCnous = Boolean.valueOf(System.getProperty("encodeCnous", prop.getProperty("encodeCrous")));
		
		primaryStage.setTitle("Esup-SGC-Client");
		primaryStage.setMinWidth(width);
		primaryStage.setMinHeight(height + 20);

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
			public void handle(WindowEvent event) {
				mainPane.exit();
				System.exit(0);
			}
		});
		mainPane.initUi();
		mainPane.changeTextPrincipal("Chargement...", "orange");
		mainPane.buttonRestart.setVisible(false);
		mainPane.buttonExit.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				stop();
			}
		});

		primaryStage.setOnCloseRequest(new EventHandler<WindowEvent>() {
	          public void handle(WindowEvent we) {
	        	  stop();
	          }
	      });  
		
		mainPane.nfcTagPane.getChildren().add(new EsupNfcClientStackPane(esupNfcTagServerUrl, getMacAddress()));

		authentification(primaryStage);
	}
	
	public void authentification(final Stage primaryStage) {
		Group root = new Group();
		root.getChildren().add(mainPane);
		final Scene scene = new Scene(root, width, height, backgroundColor);
		primaryStage.setScene(scene);
		primaryStage.show();

		Task<Void> task = new Task<Void>() {
		    @Override 
		    public Void call() {
		    	while(true) {
		    		numeroId = FileLocalStorage.getItem("numeroId");
				    if(numeroId != null && !numeroId.toString().equals("") && !"undefined".equals(numeroId) && !"null".equals(numeroId)){
				    	eppnInit = FileLocalStorage.getItem("eppnInit");
				    	break;
				    }
		    		Utils.sleep(1000);
		    	}
		    	return null;
		    }
		};
		Thread th = new Thread(task);
        th.setDaemon(true);
        th.start();
        
		task.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event) {
				launchClient(primaryStage);
			}
		});
		
	}
	
	@Override
	public void stop(){
		mainPane.exit();
		System.exit(0);
	 	}
	
	public void launchClient(final Stage primaryStage) {
	
		ClientCheckService clientCheckService = new ClientCheckService(mainPane);
		Thread clientCheckThread = new Thread(clientCheckService);
		clientCheckThread.setDaemon(true);
		clientCheckThread.start();		
		

		WaitClientReadyTask waitClientReadyTask = new WaitClientReadyTask();
		waitClientReadyTask.clientReady.bind(clientCheckService.clientReady);
		waitClientReadyTask.setOnSucceeded(new EventHandler<WorkerStateEvent>() {
			@Override
			public void handle(WorkerStateEvent t) {
				mainPane.setOk();
				mainService.start();				
			}
		});
		
		clientCheckService.setOnSucceeded(new EventHandler<WorkerStateEvent>(){
			@Override
			public void handle(WorkerStateEvent event) {
				log.info("client check ended");
				Thread waitClientReadyThread = new Thread(waitClientReadyTask);
				waitClientReadyThread.setDaemon(true);
				waitClientReadyThread.start();		
			}
		});
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
