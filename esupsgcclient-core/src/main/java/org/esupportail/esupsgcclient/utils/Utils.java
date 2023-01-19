package org.esupportail.esupsgcclient.utils;

import org.apache.log4j.Logger;

import java.net.NetworkInterface;
import java.net.SocketException;
import java.util.Enumeration;

@SuppressWarnings("restriction")
public class Utils {

	private final static Logger log = Logger.getLogger(Utils.class);

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
	
}
