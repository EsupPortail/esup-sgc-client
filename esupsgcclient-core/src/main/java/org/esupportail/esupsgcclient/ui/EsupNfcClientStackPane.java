package org.esupportail.esupsgcclient.ui;

import javax.annotation.Resource;

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
import java.util.concurrent.atomic.AtomicBoolean;

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

    @Resource
    LogTextAreaService logTextAreaService;

    public void init() throws HeadlessException {
            webView.getEngine().setJavaScriptEnabled(true);
            if (fileLocalStorage.getItem("numeroId") != null) {
                appSession.setNumeroId(fileLocalStorage.getItem("numeroId"));
            }
            String url = appConfig.getEsupNfcTagServerUrl() + "/nfc-index?numeroId=" + appSession.getNumeroId() + "&jarVersion=" + getJarVersion() + "&imei=esupSgcClient&macAddress=" + Utils.getMacAddress();
            logTextAreaService.appendText("webView load : " + url);
            webView.getEngine().getLoadWorker().stateProperty().addListener((observable, oldValue, newValue) -> {
                logTextAreaService.appendText("webView state : " + newValue);
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
                            } catch (IOException e) {
                                log.error("jar download error", e);
                            }
                        }
                    }
                }

            });
        StackPane webviewPane = new StackPane(webView);
        getChildren().add(webviewPane);
    }

    public void readLocalStorage() {
        new Thread(() -> {
            AtomicBoolean windowsVarsNotReady = new AtomicBoolean(true);
        while (windowsVarsNotReady.get()) {
            Utils.jfxRunLaterIfNeeded(() -> {
                JSObject window = (JSObject) webView.getEngine().executeScript("window");
                if (window.getMember("numeroId") != null && !"".equals(window.getMember("numeroId")) && !"undefined".equals(window.getMember("numeroId"))) {
                    appSession.setNumeroId(window.getMember("numeroId").toString());
                    fileLocalStorage.setItem("numeroId", appSession.getNumeroId());
                } else {
                    windowsVarsNotReady.set(false);
                }
                if (window.getMember("sgcAuthToken") != null && !"".equals(window.getMember("sgcAuthToken")) && !"undefined".equals(window.getMember("sgcAuthToken"))) {
                    appSession.setSgcAuthToken(window.getMember("sgcAuthToken").toString());
                    fileLocalStorage.setItem("sgcAuthToken", appSession.getSgcAuthToken());
                } else {
                    windowsVarsNotReady.set(false);
                }
                if (window.getMember("eppnInit") != null && !"".equals(window.getMember("eppnInit")) && !"undefined".equals(window.getMember("eppnInit"))) {
                    appSession.setEppnInit(window.getMember("eppnInit").toString());
                    fileLocalStorage.setItem("eppnInit", appSession.getEppnInit());
                } else {
                    windowsVarsNotReady.set(false);
                }
                if (window.getMember("authType") != null && !"".equals(window.getMember("authType")) && !"undefined".equals(window.getMember("authType"))) {
                    appSession.setAuthType(window.getMember("authType").toString());
                    fileLocalStorage.setItem("authType", appSession.getAuthType());
                } else {
                    windowsVarsNotReady.set(false);
                }
            });
            Utils.sleep(500);
        }
        }).start();
    }

    private void readLocalStorageLoop() {
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
