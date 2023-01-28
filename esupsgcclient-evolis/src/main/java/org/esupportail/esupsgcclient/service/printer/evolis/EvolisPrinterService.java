package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.annotation.Resource;
import javafx.application.Platform;
import javafx.concurrent.Task;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.scene.control.Dialog;
import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.TilePane;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.util.Optional;


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

	@Resource
	EvolisHeartbeatTaskService evolisHeartbeatTaskService;

	@Resource
	EvolisTestPcsc evolisTestPcsc;

	@Override
	public String getMaintenanceInfo() {
		return getNextCleaningSteps().getResult();
	}

	@Override
	public void setupJfxUi(Tooltip tooltip, TextArea logTextarea, MenuBar menuBar) {
		tooltip.textProperty().bind(evolisHeartbeatTaskService.titleProperty());
		evolisHeartbeatTaskService.start();
		evolisHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> logTextarea.appendText(newValue + "\n"));

		MenuItem evolisReject = new MenuItem();
		evolisReject.setText("Rejeter la carte");
		MenuItem evolisPrintEnd = new MenuItem();
		evolisPrintEnd.setText("Clore la session d'impression");
		MenuItem evolisCommand = new MenuItem();
		evolisCommand.setText("Envoyer une commande avancée à l'imprimante");
		MenuItem testPcsc = new MenuItem();
		testPcsc.setText("Stress test pc/sc");
		Menu evolisMenu = new Menu();
		evolisMenu.setText("Evolis");
		evolisMenu.getItems().addAll(evolisReject, evolisPrintEnd, evolisCommand, testPcsc);
		menuBar.getMenus().add(evolisMenu);

		evolisReject.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				Thread th = new Thread(new Task<>() {
					@Override
					protected Object call() throws Exception {
						reject();
						return null;
					}
				});
				th.setDaemon(true);
				th.start();
			}
		});

		evolisPrintEnd.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				Thread th = new Thread(new Task<>() {
					@Override
					protected Object call() throws Exception {
						printEnd();
						return null;
					}
				});
				th.setDaemon(true);
				th.start();
			}
		});

		testPcsc.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				evolisTestPcsc.getTestPcscDialog().show();
			}
		});

		TilePane r = new TilePane();
		TextInputDialog td = new TextInputDialog("Rvtods");
		td.setHeaderText("Lancer une commande à l'imprimante evolis");
		td.setContentText("Commande");
		evolisCommand.setOnAction(new EventHandler<ActionEvent>() {
			@Override
			public void handle(ActionEvent e) {
				Optional<String> result = td.showAndWait();
				result.ifPresent(command -> {
					try {
						logTextarea.appendText("Send command to evolis : " + command + "\n");
						EvolisResponse response = sendRequest(evolisPrinterCommands.getEvolisCommandFromPlainText(command));
						logTextarea.appendText("Response from evolis : " + response.getResult() + "\n");
					} catch (EvolisSocketException ex) {
						logTextarea.appendText("Evolis exception : " + ex.getMessage() + "\n");
						log.warn(String.format("Evolis exception sending command %s : %s", command, ex.getMessage()), ex);
					}
				});
			}
		});

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
						log.trace("cmdString : " + cmdString);
						log.trace("SocketTimeoutException - response received - we stop it");
						log.trace(responseStr.length() > 200 ? responseStr.substring(0, 200) : responseStr);
						break;
					} else {
						if(System.currentTimeMillis()-time>60000) {
							// close socket - sinon evolis center reste en boucle infinie
							socketInputStream.close();
							throw new EvolisSocketException("No response of Evolis after 60 sec -> abort", null);
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
			if(e.getMessage().contains("Communication session already reserved")) {
				throw new RuntimeException("Exception when sending evolis request - Communication session already reserved : " + evolisRequest, e);
			}
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

	public void printEnd() throws EvolisSocketException {
		sendRequest(evolisPrinterCommands.printEnd());
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
	
	public void noEject() {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.noEject());
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

/*
 	Do not use :redundant with printBegin ?
 */
	public void startSequence() {
		sendRequestAndRetryIfFailed(evolisPrinterCommands.startSequence());
	}

	/*
		Do not use : redundant with printEnd ?
	 */
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
		} catch(Exception e) {
			log.warn("printEnd nos succeed : " + e.getMessage(), e);
		}
	}

	public void setup() {
		disableSomePrinterStatus();
	}

	private void disableSomePrinterStatus() {
		try {
			sendRequest(evolisPrinterCommands.disableFeederNearEmptyPrinterStatus());
		} catch (EvolisSocketException e) {
			log.warn("Cant disable driver printer status : " + e.getMessage(), e);
		}
	}

}
