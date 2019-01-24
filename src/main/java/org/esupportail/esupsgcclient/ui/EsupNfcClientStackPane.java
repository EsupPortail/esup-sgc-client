package org.esupportail.esupsgcclient.ui;

import java.awt.HeadlessException;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;

import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;

@SuppressWarnings("restriction")
public class EsupNfcClientStackPane extends StackPane {

	private final static Logger log = Logger.getLogger(EsupNfcClientStackPane.class);
	
	private String esupNfcTagServerUrl;
	
	private String numeroId = "";
	private String eppnInit;
	private String authType;
	private String readyToScan;
	
	public static WebView webView = new WebView();
	
	private JavaScriptConsoleBridge javaScriptConsoleBridge;
	

	public EsupNfcClientStackPane(String esupNfcTagUrl, final String macAdress) throws HeadlessException {
		
		esupNfcTagServerUrl = esupNfcTagUrl;
		
		Platform.runLater(new Runnable() {
			public void run() {
				webView.setPrefWidth(500);
				webView.getEngine().setJavaScriptEnabled(true);
                javaScriptConsoleBridge  = new JavaScriptConsoleBridge();
                if(FileLocalStorage.getItem("numeroId") != null) {
                	numeroId = FileLocalStorage.getItem("numeroId");
                }
            	String url = esupNfcTagServerUrl + "/nfc-index?numeroId=" + numeroId + "&jarVersion=" + getJarVersion() + "&imei=esupSgcClient&macAddress=" + macAdress;
                log.info("webView load : " + url);
                webView.getEngine().getLoadWorker().stateProperty().addListener(new ChangeListener<State>() {
					public void changed(ObservableValue<? extends State> observable, State oldValue, State newValue) {
						if (newValue == State.SUCCEEDED) {
							JSObject window = (JSObject) webView.getEngine().executeScript("window");
			                window.setMember("Android", javaScriptConsoleBridge);
		                    webView.getEngine().executeScript("window.onerror = function myErrorHandler(errorMsg, url, lineNumber) {Android.windowerror(errorMsg)}");
		                    webView.getEngine().executeScript("console.error = error => {Android.consoleerror(error)};");
		                    readLocalStorage();
		                }
					}
		        });
		        
		        webView.getEngine().getLoadWorker().exceptionProperty().addListener(new ChangeListener<Throwable>() {
		            public void changed(ObservableValue<? extends Throwable> ov, Throwable t, Throwable t1) {
		                log.error(");Received exception: "+t1.getMessage(), t1);
		            }
		        });
		        
		        webView.getEngine().load(url);		        
		        
		        webView.getEngine().locationProperty().addListener(new ChangeListener<String>() {

					public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {
	                
						if(newValue.length() >= 12){
							if("download-jar".equals(newValue.substring(newValue.length() - 12))){
								try {
									FileUtils.copyURLToFile(new URL(newValue), new File("esupnfctagdesktop.jar"));
									webView.getEngine().loadContent("<html>Téléchargment terminé dans le dossier de lancement du jar</html>", "text/html");
									webView.setPrefWidth(500);
								} catch (IOException e) {
									log.error("jar download error", e);
								}
								
							}
						}
					}

		        });
			}
		});
		StackPane webviewPane = new StackPane(webView);
		getChildren().add(webviewPane);
	}
    
    public void readLocalStorage(){
    	Platform.runLater(new Runnable() {
			public void run() {
		    	JSObject window = (JSObject) webView.getEngine().executeScript("window");
			    numeroId = window.getMember("numeroId").toString();
			    eppnInit = window.getMember("eppnInit").toString();
			    authType = window.getMember("authType").toString();
                if(numeroId != null && !numeroId.equals("") && !"undefined".equals(numeroId)){
                	FileLocalStorage.setItem("numeroId", numeroId);
                }
			}
    	});
    }
    
    public void checkReadyToScan(){
    	Platform.runLater(new Runnable() {
			public void run() {
				JSObject window = (JSObject) webView.getEngine().executeScript("window");
				readyToScan = window.getMember("readyToScan").toString();
			}
    	});
    }
    
    private String getJarVersion() {
    	BufferedReader reader = new BufferedReader(new InputStreamReader(
    		    this.getClass().getResourceAsStream("/versionJar.txt")));
        try {
        	String version = reader.readLine();
        	log.info("jar version is : " + version);
			return version;
		} catch (IOException e) {
			log.error("read version error", e);
		}
        return null;
    }
    
    public String getNumeroId(){
    	return numeroId;
    }

    public String getEppnInit(){
    	return eppnInit;
    }

    public String getAuthType(){
    	return authType;
    }
    
    public String getReadyToScan(){
    	return readyToScan;
    }
        
	public String getEsupNfcTagServerUrl() {
		return esupNfcTagServerUrl;
	}

}
