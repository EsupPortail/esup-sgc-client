package org.esupportail.esupsgcclient.ui;

import org.apache.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.annotation.Resource;

@Component
public class JavaScriptConsoleBridge {

    @Resource
    FileLocalStorage fileLocalStorage;
	
	private final static Logger log = Logger.getLogger(JavaScriptConsoleBridge.class);

	public void disconnect() {
		log.info("Javascript exit !");
        fileLocalStorage.removeItem("numeroId");
		System.exit(0);
    }
	
	public void consoleerror(String text) {
        log.info("Console Javascript : " + text);
    }
	
    public void info(String text) {
        log.info("Info Javascript : " + text);
    }

    public void windowerror(String text) {
        log.error("Window Javascript : " + text);
    }
    
    public void warn(String text) {
        log.warn("Warn Javascript : " + text);
    }
}
