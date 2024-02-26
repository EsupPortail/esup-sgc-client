package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
import javax.annotation.Resource;

import javafx.scene.control.Menu;
import javafx.scene.control.MenuBar;
import javafx.scene.control.MenuItem;
import javafx.scene.control.TextArea;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.Tooltip;
import javafx.scene.layout.TilePane;
import javafx.stage.Stage;
import org.apache.commons.codec.binary.Base64;
import org.apache.commons.io.IOUtils;
import org.esupportail.esupsgcclient.AppConfig;
import org.esupportail.esupsgcclient.AppSession;
import org.esupportail.esupsgcclient.service.printer.EsupSgcPrinterService;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.ui.EsupSgcDesfireFullTestPcscDialog;
import org.esupportail.esupsgcclient.ui.EsupSgcTestPcscDialog;
import org.esupportail.esupsgcclient.ui.evolis.EvolisTestPrintDialog;
import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStreamWriter;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;
import java.util.Optional;


/**
 * This Service provides commands for interaction with sgc from evolis printers
 * It computes JSON-RPC commands to send to Evolis Services Provider 2
 */
@Component
public class EvolisPrinterService extends EsupSgcPrinterService {
	
	final static Logger log = LoggerFactory.getLogger(EvolisPrinterService.class);

	final static long DEFAULT_TIMEOUT = 3000;

	final static long DEFAULT_TIMEOUT_PRINT = 28000;

	ObjectMapper objectMapper = new ObjectMapper();

	@Resource
	AppConfig appConfig;
	
	@Resource
	EvolisPrinterCommands evolisPrinterCommands;

	@Resource
	EvolisHeartbeatTaskService evolisHeartbeatTaskService;

	@Resource
	EsupSgcTestPcscDialog esupSgcTestPcscDialog;

	@Resource
	EsupSgcDesfireFullTestPcscDialog esupSgcDesfireFullTestPcscDialog;

	@Resource
	AppSession appSession;

	@Resource
	EvolisTestPrintDialog evolisTestPrintDialog;

	@Override
	public String getMaintenanceInfo() {
		return getNextCleaningSteps().getResult();
	}

	@Override
	public void setupJfxUi(Stage stage, Tooltip tooltip, TextArea logTextarea, MenuBar menuBar) {
		tooltip.textProperty().bind(evolisHeartbeatTaskService.titleProperty());
		evolisHeartbeatTaskService.start();
		evolisHeartbeatTaskService.titleProperty().addListener((observable, oldValue, newValue) -> Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText(newValue + "\n")));

		MenuItem evolisReject = new MenuItem();
		evolisReject.setText("Rejeter la carte");
		MenuItem evolisClearStatus = new MenuItem();
		evolisClearStatus.setText("Forcer à rafraichir le status de l'imprimante");
		MenuItem evolisPrintEnd = new MenuItem();
		evolisPrintEnd.setText("Clore la session d'impression");
		MenuItem evolisRestoreManufactureParameters = new MenuItem();
		evolisRestoreManufactureParameters.setText("Restaurer les paramètres d'usine de l'imprimante");
		MenuItem evolisRestart = new MenuItem();
		evolisRestart.setText("Redémarrer l'imprimante");
		MenuItem evolisCommand = new MenuItem();
		evolisCommand.setText("Envoyer une commande avancée à l'imprimante");
		MenuItem testPcsc = new MenuItem();
		testPcsc.setText("Stress test pc/sc");
		MenuItem pcscDesfireTest = new MenuItem();
		pcscDesfireTest.setText("Stress test PC/SC DES Blank Desfire");
		MenuItem printFakeTest = new MenuItem();
		printFakeTest.setText("Stress test print evolis");
		MenuItem stopEvolis = new MenuItem();
		stopEvolis.setText("Éteindre l'imprimante");
		Menu evolisMenu = new Menu();
		evolisMenu.setText("Evolis");
		evolisMenu.getItems().addAll(evolisReject, evolisClearStatus, evolisPrintEnd, evolisRestoreManufactureParameters,  evolisRestart, evolisCommand, testPcsc, pcscDesfireTest, printFakeTest, stopEvolis);
		menuBar.getMenus().add(evolisMenu);

		evolisReject.setOnAction(actionEvent -> {
			callCommand(evolisPrinterCommands.reject(), logTextarea);
		});

		evolisPrintEnd.setOnAction(actionEvent -> {
			callCommand(evolisPrinterCommands.printEnd(), logTextarea);
		});

		evolisClearStatus.setOnAction(actionEvent -> {
			callCommand(evolisPrinterCommands.evolisClearStatus(), logTextarea);
		});

		evolisRestoreManufactureParameters.setOnAction(actionEvent -> {
			callCommand(evolisPrinterCommands.restoreManufactureParameters(), logTextarea);
		});

		evolisRestart.setOnAction(actionEvent -> {
			callCommand(evolisPrinterCommands.evolisRestart(), logTextarea);
		});

		testPcsc.setOnAction(actionEvent -> {
			esupSgcTestPcscDialog.getTestPcscDialog(
					()->try2sendRequest(evolisPrinterCommands.insertCardToContactLessStation()),
					()->try2sendRequest(evolisPrinterCommands.eject())
			).show();
		});
		testPcsc.disableProperty().bind(appSession.nfcReadyProperty().not().or(appSession.taskIsRunningProperty()).or(appSession.printerReadyProperty().not()));

		pcscDesfireTest.setOnAction(actionEvent -> {
			esupSgcDesfireFullTestPcscDialog.getTestPcscDialog(
					()->try2sendRequest(evolisPrinterCommands.insertCardToContactLessStation()),
					()->try2sendRequest(evolisPrinterCommands.eject())
			).show();
		});
		pcscDesfireTest.disableProperty().bind(appSession.nfcReadyProperty().not().or(appSession.taskIsRunningProperty()).or(appSession.printerReadyProperty().not()));

		printFakeTest.setOnAction(actionEvent -> {
			evolisTestPrintDialog.getEvolisTestPrintDialog(this, logTextarea).show();
		});
		printFakeTest.disableProperty().bind(appSession.taskIsRunningProperty().or(appSession.printerReadyProperty().not()));

		TilePane r = new TilePane();
		TextInputDialog td = new TextInputDialog("Echo;ESUP-SGC d'ESUP-Portail");
		td.setHeaderText("Lancer une commande à l'imprimante evolis");
		td.setContentText("Commande");
		evolisCommand.setOnAction(actionEvent -> {
			Optional<String> result = td.showAndWait();
			result.ifPresent(command -> {
				callCommand(evolisPrinterCommands.getEvolisCommandFromPlainText(command), logTextarea);
			});
		});

		stopEvolis.setOnAction(actionEvent -> {
			callCommand(evolisPrinterCommands.shutdown(), logTextarea);
		});

	}

	void callCommand(EvolisRequest evolisRequest, TextArea logTextarea) {
		new Thread(() -> {
			Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Send command to evolis : " + evolisRequest + "\n"));
			try {
				EvolisResponse evolisResponse = sendRequest(evolisRequest);
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Response from evolis : " + evolisResponse.getResult() + "\n"));
			} catch (EvolisSocketException ex) {
				log.warn(String.format("Evolis exception sending command %s : %s", evolisRequest, ex.getMessage()), ex);
				Utils.jfxRunLaterIfNeeded(() -> logTextarea.appendText("Evolis exception : " + ex.getMessage() + "\n"));
			}
		}).start();
	}

	public Socket getSocket() {
		try {
			Socket socket = new Socket();
			socket.connect(new InetSocketAddress(appConfig.getPrinterEvolisIp(), appConfig.getPrinterEvolisPort()), 500);
			socket.setSoTimeout(100);
			return socket;
		} catch (IOException e) {
			throw new RuntimeException("Can't connect to evolis printer on " + appConfig.getPrinterEvolisIp() + ":" + appConfig.getPrinterEvolisPort());
		}
	}

	synchronized EvolisResponse sendRequest(EvolisRequest req, long timeout) throws EvolisSocketException {
		log.debug("Request : {}", req);
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
						if(System.currentTimeMillis()-time>timeout) {
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
			log.debug("Response : {}", response);
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

	synchronized EvolisResponse sendRequest(EvolisRequest req) throws EvolisSocketException {
		return sendRequest(req, DEFAULT_TIMEOUT);
	}

	EvolisResponse sendRequestAndRetryIfFailed(EvolisRequest evolisRequest, long timeout) {
		EvolisResponse response = null;
		try {
			response = sendRequest(evolisRequest, timeout);
		} catch (EvolisSocketException e) {
			if(e.getMessage().contains("Communication session already reserved")) {
				throw new RuntimeException("Exception when sending evolis request - Communication session already reserved : " + evolisRequest, e);
			}
			log.warn("Exception when sending evolis request (we retry it in 2 sec) : " + evolisRequest, e);
			Utils.sleep(2000);
			sendRequestAndRetryIfFailed(evolisRequest, timeout);
		}
		return response;
	}

	EvolisResponse sendRequestAndRetryIfFailed(EvolisRequest evolisRequest) {
		return sendRequestAndRetryIfFailed(evolisRequest, DEFAULT_TIMEOUT);
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
		sendRequestAndRetryIfFailed(evolisPrinterCommands.print(), DEFAULT_TIMEOUT_PRINT);
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

	void try2sendRequest(EvolisRequest evolisRequest) {
		try {
			sendRequest(evolisRequest);
		} catch(Exception e) {
			log.warn(evolisRequest + " nos succeed : " + e.getMessage(), e);
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
