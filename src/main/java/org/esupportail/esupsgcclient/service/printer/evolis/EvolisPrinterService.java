package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsgcclient.AppConfig;
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

	Socket socket;

	ObjectMapper objectMapper = new ObjectMapper();

	@Resource
	AppConfig appConfig;

	public boolean initSocket(boolean exceptionIfFailed) {
		if(socket == null || socket.isClosed() || !socket.isConnected() || socket.isClosed() || !socket.isBound()) {
			try {
				socket = new Socket(appConfig.getPrinterEvolisIp(), appConfig.getPrinterEvolisPort());
				socket.setSoTimeout(100);
				return true;
			} catch (IOException e) {
				if (exceptionIfFailed) {
					throw new RuntimeException("Can't connect to evolis printer on " + appConfig.getPrinterEvolisIp() + ":" + appConfig.getPrinterEvolisPort());
				} else{
					log.debug("Can't connect to evolis printer on {}:{}", appConfig.getPrinterEvolisIp(), appConfig.getPrinterEvolisPort());
				}
			}
			return false;
		}
		return true;
	}

	public void closeSocket() {
		try {
			if(socket!=null) {
				socket.close();
			}
		} catch (IOException e) {
			log.debug("Can't close socket");
		}
	}

	EvolisResponse sendRequest(EvolisRequest req) {
		try {
			initSocket(true);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			String cmdString = objectMapper.writeValueAsString(req) ;
			writer.write(cmdString);
			writer.flush();
			log.debug(cmdString.length()>200 ? cmdString.substring(0,200) + "..." + cmdString.substring(cmdString.length()-199) : cmdString);
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
						log.debug(responseStr.length() > 200 ? responseStr.substring(0, 200) : responseStr);
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
			throw new RuntimeException("IOException seding Request to evolis : " + req, e);
		} finally {
			closeSocket();
		}
	}
	EvolisResponse sendRequestAndLog(EvolisRequest evolisRequest) {
		log.trace("Request : {}", evolisRequest);
		EvolisResponse response = sendRequest(evolisRequest);
		log.trace("Response : {}", response);
		return response;
	}

	public EvolisResponse insertCard() {
		return sendRequestAndLog(EvolisPrinterCommands.insertCard());
	}

	public EvolisResponse getPrinterStatus() {
		return sendRequestAndLog(EvolisPrinterCommands.getPrinterStatus());
	}

	public EvolisResponse printBegin() {
		return sendRequestAndLog(EvolisPrinterCommands.printBegin());
	}

	public void printSet() {
		sendRequestAndLog(EvolisPrinterCommands.printSet());
	}

	public void printEnd() {
		sendRequestAndLog(EvolisPrinterCommands.printEnd());
	}


	public void printFrontColorBmp(String bmpColorAsBase64) {
		sendRequestAndLog(EvolisPrinterCommands.printFrontColorBmp(bmpColorAsBase64));
	}

	public void printFrontBlackBmp(String bmpBlackAsBase64) {
		sendRequestAndLog(EvolisPrinterCommands.printFrontBlackBmp(bmpBlackAsBase64));
	}

	public void printFrontVarnish(String bmpVarnishAsBase64) {
		sendRequestAndLog(EvolisPrinterCommands.printFrontVarnish(bmpVarnishAsBase64));
	}

	public void print() {
		sendRequestAndLog(EvolisPrinterCommands.print());
	}

	public EvolisResponse insertCardToContactLessStation() {
		return sendRequestAndLog(EvolisPrinterCommands.insertCardToContactLessStation());
	}


	public void eject() {
		sendRequestAndLog(EvolisPrinterCommands.eject());
	}

	public void reject() {
		sendRequestAndLog(EvolisPrinterCommands.reject());
	}

	public void startSequence() {
		sendRequestAndLog(EvolisPrinterCommands.startSequence());
	}

	public void endSequence() {
		sendRequestAndLog(EvolisPrinterCommands.endSequence());
	}
}
