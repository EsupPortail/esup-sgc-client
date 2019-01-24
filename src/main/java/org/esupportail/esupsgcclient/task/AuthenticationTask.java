package org.esupportail.esupsgcclient.task;

import java.util.HashMap;
import java.util.Map;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.esupportail.esupsgcclient.ui.FileLocalStorage;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Task;
import netscape.javascript.JSObject;

@SuppressWarnings("restriction")
public class AuthenticationTask extends Task<Map<String, String>> {
	
	private final static Logger log = Logger.getLogger(AuthenticationTask.class);


	private Map<String, String> params = new HashMap<String, String>();
	private JSObject window;
	
	public AuthenticationTask(JSObject window) {
		super();
		this.window = window;
	}

	@Override
	protected Map<String, String> call() throws Exception {
		while(true) {
			if(params.get("numeroId") != null && !params.get("numeroId").equals("") && !"undefined".equals(params.get("numeroId"))) {
				break;
			} else {
				System.err.println("test : " + params.get("numeroId"));
				readLocalStorage();
			}
			Utils.sleep(1000);
		}
		return params;
	}

    public void readLocalStorage(){
    	try{
			Object numeroId = EsupSGCClientApplication.window.getMember("numeroId");
			System.err.println(numeroId);
		    if(numeroId != null && !numeroId.toString().equals("") && !"undefined".equals(numeroId.toString())){
				params.put("numeroId", numeroId.toString());
				params.put("eppnInit", window.getMember("eppnInit").toString());
				FileLocalStorage.setItem("numeroId", params.get("numeroId"));
		    }
    	}catch (Exception e) {
    		Utils.sleep(1000);
    		//log.warn("tentative de lecture du local storage", e);
    	}
			
    }
	
}
