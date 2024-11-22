package org.esupportail.esupsgcclient.service.cnous;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;

public class CnousFournisseurCarteRunExe {

	private final static Logger log = LoggerFactory.getLogger(CnousFournisseurCarteRunExe.class);
	
	private String pathToExe;
	
	public CnousFournisseurCarteRunExe(String pathToExe) {
		this.pathToExe = pathToExe;
	}

	public boolean check() throws CnousFournisseurCarteException{
		String readResult = "";
		ProcessBuilder processBuilder;
		Process process;
		try {
			processBuilder = new ProcessBuilder(pathToExe + "\\CreationCarteCrous.exe", "-t");
			processBuilder.directory(new File(pathToExe));
			process = processBuilder.start(); 
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = input.readLine()) != null) {
				  readResult += line;
				}
		} catch (IOException e) {
			log.error("cnous api not ready", e);
			throw new CnousFournisseurCarteException("cnous api not ready", e);
		}
		if(readResult.equals("true")){
			log.info("cnous api ready");
			return true;
		}else{
			log.warn("cnous api not ready : " + readResult);
			return false;
		}
	}
	
	public String readCard(){
		String readResult = "";
		ProcessBuilder processBuilder;
		Process process;
		try {
			processBuilder = new ProcessBuilder(pathToExe + "\\CreationCarteCrous.exe", "-l");
			processBuilder.directory(new File(pathToExe));
			process = processBuilder.start(); 
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = input.readLine()) != null) {
				  readResult += line;
				}
		} catch (IOException e1) {
			e1.printStackTrace();
		}
		return readResult;
	}

	public String writeCard(String numCard) throws CnousFournisseurCarteException{
		String finalNumCard = ("000000000000000" + numCard).substring(numCard.toString().length());
		log.info("cnous id to encode : "+finalNumCard);
		String readResult = "";
		ProcessBuilder processBuilder;
		Process process;
		try {
			processBuilder = new ProcessBuilder(pathToExe + "\\CreationCarteCrous.exe", "-e", finalNumCard);
			processBuilder.directory(new File(pathToExe));
			process = processBuilder.start(); 
			BufferedReader input = new BufferedReader(new InputStreamReader(process.getInputStream()));
			String line;
			while ((line = input.readLine()) != null) {
				  readResult += line;
				}
		} catch (IOException e) {
			log.error("cnous encoding error",e);
			throw new CnousFournisseurCarteException("cnous encoding error",e);
		}
		return readResult;
	}
	
}
