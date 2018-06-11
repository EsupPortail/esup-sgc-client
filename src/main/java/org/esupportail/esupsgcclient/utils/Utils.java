package org.esupportail.esupsgcclient.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;
import javafx.util.Duration;

@SuppressWarnings("restriction")
public class Utils {
	
    public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
		}
	}

    public static HttpComponentsClientHttpRequestFactory clientHttpRequestFactory() {
    	HttpComponentsClientHttpRequestFactory factory = new HttpComponentsClientHttpRequestFactory();
        factory.setReadTimeout(10000);
        factory.setConnectTimeout(10000);
        return factory;
    }
    
	public static String getExceptionString(Throwable throwable) {
		StringWriter sw = new StringWriter();
		PrintWriter pw = new PrintWriter(sw);
		throwable.printStackTrace(pw);
		return sw.toString();
	}
	
	public static void playSound(String soundFile) {
		Media media = new Media( EsupSGCClientApplication.class.getResource("/sound/" + soundFile).toExternalForm());
		MediaPlayer player = new MediaPlayer(media);
		player.setStopTime(Duration.seconds(2));
		player.setAutoPlay(true);
	}
	
}
