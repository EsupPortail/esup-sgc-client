package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
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
public class EvolisPrinterService extends EsupSgcPrinterService {
	
	final static Logger log = LoggerFactory.getLogger(EvolisPrinterService.class);

	ObjectMapper objectMapper = new ObjectMapper();

	@Resource
	AppConfig appConfig;
	
	@Resource
	EvolisPrinterCommands evolisPrinterCommands;

	@Override
	public String getMaintenanceInfo() {
		return getNextCleaningSteps().getResult();
	}

	public Socket getSocket() {
		try {
			Socket socket = new Socket(appConfig.getPrinterEvolisIp(), appConfig.getPrinterEvolisPort());
			socket.setSoTimeout(100);
			return socket;
		} catch (IOException e) {
			throw new RuntimeException("Can't connect to evolis printer on " + appConfig.getPrinterEvolisIp() + ":" + appConfig.getPrinterEvolisPort());
		}
	}

	synchronized EvolisResponse sendRequest(EvolisRequest req) throws EvolisSocketException {
		Socket socket = null;
		try {
			socket = getSocket();
			BufferedWriter writer = new BufferedWriter(new OutputStreamWriter(socket.getOutputStream()));
			String cmdString = objectMapper.writeValueAsString(req) ;
			writer.write(cmdString);
			writer.flush();
			InputStream socketInputStream= socket.getInputStream();
			String responseStr ="";
			long time = System.currentTimeMillis();
			int k = 1;
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
					} else {
						if(System.currentTimeMillis()-time>30000) {
							// close socket - sinon evolis center reste en boucle infinie
							socketInputStream.close();
							throw new EvolisSocketException("No response of Evolis after 30 sec -> abort", null);
						} else if(System.currentTimeMillis()-time>1000*k) {
							k++;
							log.debug("SocketTimeoutException after " + k + " sec - but no response - we continue ...");
						}
					}
					Utils.sleep(50);
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
		return sendRequestAndRetryIfFailed(evolisPrinterCommands.insertCard());
	}

	public EvolisResponse getPrinterStatus() throws EvolisSocketException {
		return sendRequest(evolisPrinterCommands.getPrinterStatus());
	}

	public EvolisResponse printBegin() {
		return sendRequestAndRetryIfFailed(evolisPrinterCommands.printBegin());
	}

	public void printSet() {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.printSet(appConfig.getPrinterEvolisSet()));
	}

	public void printEnd() {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.printEnd());
	}


	public void printFrontColorBmp(String bmpColorAsBase64) {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.printFrontColorBmp(bmpColorAsBase64));
	}

	public void printFrontBlackBmp(String bmpBlackAsBase64) {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.printFrontBlackBmp(bmpBlackAsBase64));
	}

	public void printFrontVarnish(String bmpVarnishAsBase64) {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.printFrontVarnish(bmpVarnishAsBase64));
	}

	public void print() {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.print());
	}

	public EvolisResponse insertCardToContactLessStation(EsupSgcTask esupSgcTask) {
		EvolisResponse response = sendRequestAndRetryIfFailed(evolisPrinterCommands.insertCardToContactLessStation());
		while(!"OK".equals(response.getResult())) {
			log.warn("Pb inserting card to contactless station : " + response);
			esupSgcTask.updateTitle4thisTask("Pb inserting card to contactless station : " + response.getResult());
			if(esupSgcTask.isCancelled()) {
				throw new RuntimeException("EvolisTask is cancelled");
			}
			Utils.sleep(2000);
			response = sendRequestAndRetryIfFailed(evolisPrinterCommands.insertCardToContactLessStation());;
		}
		return response;
	}


	public void eject() {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.eject());
	}

	public void reject() {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.reject());
	}

	public void startSequence() {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.startSequence());
	}

	public void endSequence() {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.endSequence());
	}

	public EvolisResponse getNextCleaningSteps() {
		try {
			return sendRequest(evolisPrinterCommands.getNextCleaningSteps());
		} catch (Exception e) {
			log.info("pb avec evolis - wait 5 sec", e);
			Utils.sleep(5000);
		}
		return null;
	}

	public void try2printEnd() {
		try {
			printEnd();
		} catch(EvolisException e) {
			log.trace("printEnd nos succeed : " + e.getMessage(), e);
		}
	}
}
