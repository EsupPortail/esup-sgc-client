package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import javax.annotation.Resource;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;


/**
 * This Service provides commands for interaction with sgc from evolis printers
 * It computes JSON-RPC commands to send to Evolis Services Provider 2
 */
@Component
public class EvolisPrinterService {
	
	final static Logger log = LoggerFactory.getLogger(EvolisPrinterService.class);

	ObjectMapper objectMapper = new ObjectMapper();

	@Resource
	AppConfig appConfig;

	public Socket getSocket() {
		try {
			Socket socket = new Socket(appConfig.getPrinterEvolisIp(), appConfig.getPrinterEvolisPort());
			socket.setSoTimeout(100);
			return socket;
		} catch (IOException e) {
			throw new RuntimeException("Can't connect to evolis printer on " + appConfig.getPrinterEvolisIp() + ":" + appConfig.getPrinterEvolisPort());
		}
	}

	EvolisResponse sendRequest(EvolisRequest req) throws EvolisSocketException {
		Socket socket = null;
		try {
			socket = getSocket();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			String cmdString = objectMapper.writeValueAsString(req) ;
			writer.write(cmdString);
			writer.flush();
			InputStream socketInputStream= socket.getInputStream();
			String responseStr ="";
			while (true) {
				int n;
				try {
					byte[] buffer = new byte[1024];
					n = socketInputStream.read(buffer);
					responseStr = responseStr + new String(buffer);
				} catch(SocketTimeoutException | SocketException ex){
					if(StringUtils.hasText(responseStr)) {
						log.trace("SocketTimeoutException - response received - we stop it");
						log.trace(responseStr.length() > 200 ? responseStr.substring(0, 200) : responseStr);
						break;
					}
				}
			}
			EvolisResponse response = objectMapper.readValue(responseStr, EvolisResponse.class);
			// close socket - sinon evolis center reste en boucle infinie
			socketInputStream.close();
			if(response.getError()!=null) {
				throw new EvolisException(response.getError());
			}
			return response;
		} catch (IOException e) {
			throw new EvolisSocketException("IOException sending Request to evolis : " + req, e);
		} finally {
			if(socket!=null) {
				try {
					socket.close();
				} catch (IOException e) {
					log.debug("can't close evolis socket", e);
				}
			}
		}
	}
	EvolisResponse sendRequestAndRetryIfFailed(EvolisRequest evolisRequest) {
		log.debug("Request : {}", evolisRequest);
		EvolisResponse response = null;
		try {
			response = sendRequest(evolisRequest);
		} catch (EvolisSocketException e) {
			log.warn("Exception when sending evolis request (we retry it in 2 sec) : " + evolisRequest, e);
			Utils.sleep(2000);
			sendRequestAndRetryIfFailed(evolisRequest);
		}
		log.debug("Response : {}", response);
		return response;
	}

	public EvolisResponse insertCard() {
		return sendRequestAndRetryIfFailed(EvolisPrinterCommands.insertCard());
	}

	public EvolisResponse getPrinterStatus() throws EvolisSocketException {
		return sendRequest(EvolisPrinterCommands.getPrinterStatus());
	}

	public EvolisResponse printBegin() {
		return sendRequestAndRetryIfFailed(EvolisPrinterCommands.printBegin());
	}

	public void printSet() {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.printSet(appConfig.getPrinterEvolisSet()));
	}

	public void printEnd() {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.printEnd());
	}


	public void printFrontColorBmp(String bmpColorAsBase64) {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.printFrontColorBmp(bmpColorAsBase64));
	}

	public void printFrontBlackBmp(String bmpBlackAsBase64) {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.printFrontBlackBmp(bmpBlackAsBase64));
	}

	public void printFrontVarnish(String bmpVarnishAsBase64) {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.printFrontVarnish(bmpVarnishAsBase64));
	}

	public void print() {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.print());
	}

	public EvolisResponse insertCardToContactLessStation() {
		EvolisResponse response = sendRequestAndRetryIfFailed(EvolisPrinterCommands.insertCardToContactLessStation());
		while(!"OK".equals(response.getResult())) {
			log.warn("Pb inserting card to caonctact less stattion : " + response);
			Utils.sleep(2000);
			response = sendRequestAndRetryIfFailed(EvolisPrinterCommands.insertCardToContactLessStation());;
		}
		return response;
	}


	public void eject() {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.eject());
	}

	public void reject() {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.reject());
	}

	public void startSequence() {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.startSequence());
	}

	public void endSequence() {
		sendRequestAndRetryIfFailed(EvolisPrinterCommands.endSequence());
	}
}
