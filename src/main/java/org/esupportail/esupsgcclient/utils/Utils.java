package org.esupportail.esupsgcclient.utils;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

import javafx.scene.media.Media;
import javafx.scene.media.MediaPlayer;

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
			Media audio = new Media(EsupSGCClientApplication.class.getResource("/sound/" + soundFile).toString());
            MediaPlayer mediaPlayer = new MediaPlayer(audio);
            mediaPlayer.play();
		} catch (Exception e) {
			log.error("error on playSound", e);
		}
	}


	public static String getMacAddress() {
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
