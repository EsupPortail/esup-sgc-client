package org.esupportail.esupsgcclient.utils;

import javafx.application.Platform;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@SuppressWarnings("restriction")
public class Utils {

	private final static Logger log = LoggerFactory.getLogger(Utils.class);

    public static void sleep(long time) {
		try {
			Thread.sleep(time);
		} catch (InterruptedException e) {
			log.trace("exception when waiting " + time + " ms ...", e);
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

	public static void jfxRunLaterIfNeeded(Runnable runnable) {
		if(Platform.isFxApplicationThread()) {
			runnable.run();
		} else {
			Platform.runLater(runnable);
		}
	}
	
}
