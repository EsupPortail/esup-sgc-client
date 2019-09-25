package org.esupportail.esupsgcclient.ui;

import java.io.EOFException;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.Resource;

public class FileLocalStorage {

	private final static Logger log = Logger.getLogger(FileLocalStorage.class);
 
	private static File file = initLocalStorageFile();
	
	private static File initLocalStorageFile() {
		String localStorageName = "esupSgcLocalStorage";
		Properties prop = new Properties();
		Resource resource = new ClassPathResource("esupsgcclient.properties");
		try {
			prop.load(resource.getInputStream());
			log.info("load props");
		} catch (IOException e1) {
			log.error("props not found");
		} 
		String localStorageDir = System.getProperty("localStorageDir", prop.getProperty("localStorageDir"));
		String OS = System.getProperty("os.name").toLowerCase();
		File file;
		File directory = new File(String.valueOf(System.getProperty("user.home")+ localStorageDir));
		if(!directory.exists()){
			directory.mkdir();
		}
		if (OS.indexOf("win") >= 0) {
			file = new File(System.getProperty("user.home")+ localStorageDir + localStorageName);
		} else {
			file = new File(localStorageName);
		}
		return file;
	}
	
	public static String getItem(String key) {
		log.info("get key : " + key);
		String value = "";
		try {
			file.createNewFile();
			FileInputStream fis = new FileInputStream(file);
			ObjectInputStream ois = new ObjectInputStream(fis);
			HashMap<String, String> item = (HashMap<String, String>) ois.readObject();
			ois.close();
			fis.close();
			value = item.get(key);
		} catch (EOFException e) {
			log.warn("error on read localstorage");
		} catch (Exception e) {
			log.error("error on read/create localstorage", e);
		}
		return value;
	}

	public static void setItem(String key, String value) {
		Map<String, String> item = new HashMap<String, String>();
		try {
			file.createNewFile();
			FileInputStream fis = new FileInputStream(file);
			try {
				ObjectInputStream ois = new ObjectInputStream(fis);
				item = (HashMap<String, String>) ois.readObject();
				ois.close();
			} catch (Exception e) {
				log.warn("error on read localstorage");
			}
			fis.close();
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			item.put(key, value);
			oos.writeObject(item);
			oos.flush();
			oos.close();
			fos.close();
			log.info(key + "=" + value + " write to localstorage");
		} catch (IOException e) {
			log.error("error on write to localstorage", e);
		}
	}

	public static void removeItem(String key) {
		log.info("remove : " + key);
		Map<String, String> item = new HashMap<String, String>();
		try {
			try {
				FileInputStream fis = new FileInputStream(file);
				ObjectInputStream ois = new ObjectInputStream(fis);
				item = (HashMap<String, String>) ois.readObject();
				ois.close();
				fis.close();
				item.remove(key);
			} catch (ClassNotFoundException e) {
				log.warn("error on read localstorage");
			}
			FileOutputStream fos = new FileOutputStream(file);
			ObjectOutputStream oos = new ObjectOutputStream(fos);
			oos.writeObject(item);
			oos.flush();
			oos.close();
			fos.close();
		} catch (IOException e) {
			log.error("error on remove localstorage", e);
		}
	}

	public void clear() {
		file.delete();
	}

}
