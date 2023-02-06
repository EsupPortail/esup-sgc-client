package org.esupportail.esupsgcclient.ui;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.concurrent.Worker.State;
import javafx.scene.layout.StackPane;
import javafx.scene.web.WebView;
import netscape.javascript.JSObject;
import org.apache.commons.io.FileUtils;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Component;

import java.awt.*;
import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;

@Component
public class EsupNfcClientStackPane extends StackPane {

    private final static Logger log = Logger.getLogger(EsupNfcClientStackPane.class);

    WebView webView = new WebView();

    @Resource
    JavaScriptConsoleBridge javaScriptConsoleBridge;

    @Resource
    FileLocalStorage fileLocalStorage;

    @Resource
    AppSession appSession;

    @Resource
    AppConfig appConfig;

    @PostConstruct
    public void init() throws HeadlessException {

        Platform.runLater(() -> {
            webView.setPrefWidth(500);
            webView.getEngine().setJavaScriptEnabled(true);
            if (fileLocalStorage.getItem("numeroId") != null) {
                appSession.setNumeroId(fileLocalStorage.getItem("numeroId"));
            }
            String url = appConfig.getEsupNfcTagServerUrl() + "/nfc-index?numeroId=" + appSession.getNumeroId() + "&jarVersion=" + getJarVersion() + "&imei=esupSgcClient&macAddress=" + Utils.getMacAddress();
            log.info("webView load : " + url);
            webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
				if (newValue == State.SUCCEEDED) {
					JSObject window = (JSObject) webView.getEngine().executeScript("window");
					window.setMember("Android", javaScriptConsoleBridge);
					webView.getEngine().executeScript("window.onerror = function myErrorHandler(errorMsg, url, lineNumber) {Android.windowerror(errorMsg)}");
					webView.getEngine().executeScript("console.error = error => {Android.consoleerror(error)};");
				}
				readLocalStorage();
			});

            webView.getEngine().getLoadWorker().exceptionProperty().addListener((ov, t, t1) -> log.error(");Received exception: " + t1.getMessage(), t1));

            webView.getEngine().load(url);

            webView.getEngine().locationProperty().addListener(new ChangeListener<String>() {

                public void changed(ObservableValue<? extends String> observable, String oldValue, String newValue) {

                    if (newValue.length() >= 12) {
                        if ("download-jar".equals(newValue.substring(newValue.length() - 12))) {
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
        });
        StackPane webviewPane = new StackPane(webView);
        getChildren().add(webviewPane);
    }

    public void readLocalStorage() {
        Platform.runLater(() -> {
            JSObject window = (JSObject) webView.getEngine().executeScript("window");
            appSession.setNumeroId(window.getMember("numeroId").toString());
            appSession.setSgcAuthToken(window.getMember("sgcAuthToken").toString());
            appSession.setEppnInit(window.getMember("eppnInit").toString());
            appSession.setAuthType(window.getMember("authType").toString());
            if (appSession.getNumeroId() != null && !appSession.getNumeroId().equals("") && !"undefined".equals(appSession.getNumeroId())) {
                fileLocalStorage.setItem("numeroId", appSession.getNumeroId());
            }
            if (appSession.getSgcAuthToken() != null && !appSession.getSgcAuthToken().equals("") && !"undefined".equals(appSession.getSgcAuthToken())) {
                fileLocalStorage.setItem("sgcAuthToken", appSession.getSgcAuthToken());
            }
            if (appSession.getEppnInit() != null && !appSession.getEppnInit().equals("") && !"undefined".equals(appSession.getEppnInit())) {
                fileLocalStorage.setItem("eppnInit", appSession.getEppnInit());
            }
            if (appSession.getAuthType() != null && !appSession.getAuthType().equals("") && !"undefined".equals(appSession.getAuthType())) {
                fileLocalStorage.setItem("authType", appSession.getAuthType());
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

}
