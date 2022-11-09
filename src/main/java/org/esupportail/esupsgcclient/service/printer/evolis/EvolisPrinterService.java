package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.client.RestTemplate;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


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

	static boolean initSocket() {
		try {
			socket = new Socket(ip,port);
			return true;
		} catch (IOException e) {
			log.debug("Can't connect to evolis printer on {}:{}", ip, port);
		}
		return false;
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
			if(socket == null || !socket.isConnected() || socket.isClosed()) {
				initSocket();
			}
			BufferedReader br = new BufferedReader(new InputStreamReader(socket.getInputStream()));
			DataOutputStream out = new DataOutputStream(socket.getOutputStream());
			byte[] byteRequest = objectMapper.writeValueAsBytes(req);
			out.write(byteRequest);
			out.flush();
			char[] data = new char[1024];
			String responseValue = "";
			while( (br.read(data)) != -1 ) {
				responseValue += data;
			}
			EvolisResponse response = objectMapper.readValue(responseValue, EvolisResponse.class);
			return response;
		} catch (IOException e) {
			log.debug("Can't close socket");
		}
		return null;
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

}
