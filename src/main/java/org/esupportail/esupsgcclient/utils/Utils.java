package org.esupportail.esupsgcclient.utils;

import java.io.PrintWriter;
import java.io.StringWriter;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import sun.audio.AudioPlayer;
import sun.audio.AudioStream;

@SuppressWarnings("restriction")
public class Utils {

	private final static Logger log = Logger.getLogger(Utils.class);

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
		try {
		    AudioStream audioStream = new AudioStream(EsupSGCClientApplication.class.getResourceAsStream("/sound/" + soundFile));
		    AudioPlayer.player.start(audioStream);
		} catch (Exception e) {
			log.error("error on playSound", e);
		}
	}
	
}
