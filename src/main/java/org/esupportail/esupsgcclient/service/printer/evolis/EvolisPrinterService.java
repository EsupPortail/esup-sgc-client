package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.util.Random;


/**
 * This Service provides commands for interaction with sgc from evolis printers
 * It computes JSON-RPC commands to send to Evolis Services Provider 2
 */
public class EvolisPrinterService {
	
	final static Logger log = LoggerFactory.getLogger(EvolisPrinterService.class);

	static String ip = "127.0.0.1";

	static int port = 18000;

	static Socket socket;

	static ObjectMapper objectMapper = new ObjectMapper();

	public static boolean initSocket(boolean exceptionIfFailed) {
		if(socket == null || socket.isClosed() || !socket.isConnected() || socket.isClosed() || !socket.isBound()) {
			try {
				socket = new Socket(ip, port);
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

	public void closeSocket() {
		try {
			if(socket!=null) {
				socket.close();
			}
		} catch (IOException e) {
			log.debug("Can't close socket");
		}
	}

	static EvolisResponse sendRequest(EvolisRequest req) {
		try {
			initSocket(true);
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			String cmdString = objectMapper.writeValueAsString(req) ;
			cmdString = cmdString.replaceAll("\n", "");
			writer.write(cmdString);
			writer.newLine();
			writer.flush();
			BufferedReader reader = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			String responseValue = reader.readLine();
			EvolisResponse response = objectMapper.readValue(responseValue, EvolisResponse.class);
			socket.close();
			return response;
		} catch (IOException e) {
			throw new RuntimeException("IOException seding Request to evolis : " + req, e);
		}
	}
	static void sendRequestAndLog(EvolisRequest evolisRequest) {
		log.debug("Request : {}", evolisRequest);
		EvolisResponse response = sendRequest(evolisRequest);
		log.debug("Response : {}", response);
	}

	public static void print(String bmpColorAsBase64, String bmpBlackAsBase64, String bmpVarnishAsBase64) {
		sendRequestAndLog(EvolisPrinterCommands.insertCard());
		sendRequestAndLog(EvolisPrinterCommands.printFrontColorBmp(bmpColorAsBase64));
		sendRequestAndLog(EvolisPrinterCommands.printFrontBlackBmp(bmpBlackAsBase64));
		sendRequestAndLog(EvolisPrinterCommands.printFrontVarnish(bmpVarnishAsBase64));
		sendRequestAndLog(EvolisPrinterCommands.print());
		sendRequestAndLog(EvolisPrinterCommands.insertCardToContactLessStation());
	}

	public static void eject() {
		sendRequestAndLog(EvolisPrinterCommands.eject());
	}

	public static void reject() {
		sendRequestAndLog(EvolisPrinterCommands.reject());
	}
}
