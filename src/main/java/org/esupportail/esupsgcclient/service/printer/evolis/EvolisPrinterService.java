package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.InterruptedIOException;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.util.Random;


/**
 * This Service provides commands for interaction with sgc from evolis printers
 * It computes JSON-RPC commands to send to Evolis Services Provider 2
 */
public class EvolisPrinterService {
	
	final static Logger log = LoggerFactory.getLogger(EvolisPrinterService.class);

	static String ip = "10.197.1.71";
	//static String ip = "127.0.0.1";

	static int port = 18000;

	static Socket socket;

	static ObjectMapper objectMapper = new ObjectMapper();

	public static boolean initSocket(boolean exceptionIfFailed) {
		if(socket == null || socket.isClosed() || !socket.isConnected() || socket.isClosed() || !socket.isBound()) {
			try {
				socket = new Socket(ip, port);
				socket.setSoTimeout(100);
				return true;
			} catch (IOException e) {
				if (exceptionIfFailed) {
					throw new RuntimeException("Can't connect to evolis printer on " + ip + ":" + port);
				} else{
					log.debug("Can't connect to evolis printer on {}:{}", ip, port);
				}
			}
			return false;
		}
		return true;
	}

	public static void closeSocket() {
		try {
			if(socket!=null) {
				socket.close();
			}
		} catch (IOException e) {
			log.debug("Can't close socket");
		}
	}

	static synchronized EvolisResponse sendRequest(EvolisRequest req) {
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
				} catch(SocketTimeoutException ex){
					if(StringUtils.hasText(responseStr)) {
						log.debug("SocketTimeoutException - response received - we stop it");
						log.warn(responseStr.length() > 200 ? responseStr.substring(0, 200) : responseStr);
						break;
					}
				}
			}
			EvolisResponse response = objectMapper.readValue(responseStr, EvolisResponse.class);
			// close socket - sinon evolis center reste en boucle infinie
			socketInputStream.close();
			return response;
		} catch (IOException e) {
			throw new RuntimeException("IOException seding Request to evolis : " + req, e);
		} finally {
			closeSocket();
		}
	}
	static EvolisResponse sendRequestAndLog(EvolisRequest evolisRequest) {
		log.trace("Request : {}", evolisRequest);
		EvolisResponse response = sendRequest(evolisRequest);
		log.trace("Response : {}", response);
		return response;
	}

	public static EvolisResponse insertCard() {
		return sendRequestAndLog(EvolisPrinterCommands.insertCard());
	}

	public static EvolisResponse getPrinterStatus() {
		return sendRequestAndLog(EvolisPrinterCommands.getPrinterStatus());
	}

	public static EvolisResponse printBegin() {
		EvolisResponse response = sendRequestAndLog(EvolisPrinterCommands.printBegin());
		if(response.getError()!=null && response.getError().getCode()==1700) {
			// 1700 == Printing session already ...
			log.warn(response.getError().getMessage());
			printEnd();
			response = sendRequestAndLog(EvolisPrinterCommands.printBegin());
		}
		return response;
	}

	public static void printSet() {
		sendRequestAndLog(EvolisPrinterCommands.printSet());
	}

	public static void printEnd() {
		sendRequestAndLog(EvolisPrinterCommands.printEnd());
	}


	public static void printFrontColorBmp(String bmpColorAsBase64) {
		sendRequestAndLog(EvolisPrinterCommands.printFrontColorBmp(bmpColorAsBase64));
	}

	public static void printFrontBlackBmp(String bmpBlackAsBase64) {
		sendRequestAndLog(EvolisPrinterCommands.printFrontBlackBmp(bmpBlackAsBase64));
	}

	public static void printFrontVarnish(String bmpVarnishAsBase64) {
		sendRequestAndLog(EvolisPrinterCommands.printFrontVarnish(bmpVarnishAsBase64));
	}

	public static void print() {
		sendRequestAndLog(EvolisPrinterCommands.print());
	}

	public static EvolisResponse insertCardToContactLessStation() {
		return sendRequestAndLog(EvolisPrinterCommands.insertCardToContactLessStation());
	}


	public static void eject() {
		sendRequestAndLog(EvolisPrinterCommands.eject());
	}

	public static void reject() {
		sendRequestAndLog(EvolisPrinterCommands.reject());
	}
}
