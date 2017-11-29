package org.esupportail.esupsgcclient.utils;

import org.springframework.http.client.HttpComponentsClientHttpRequestFactory;

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
	
}
