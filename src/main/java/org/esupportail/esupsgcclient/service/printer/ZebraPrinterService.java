package org.esupportail.esupsgcclient.service.printer;

import java.awt.Color;
import java.util.List;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.EsupSGCClientApplication;
import org.esupportail.esupsgcclient.utils.Utils;

import com.zebra.sdk.comm.Connection;
import com.zebra.sdk.comm.ConnectionException;
import com.zebra.sdk.common.card.comm.internal.CardError;
import com.zebra.sdk.common.card.containers.JobStatus;
import com.zebra.sdk.common.card.containers.JobStatusInfo;
import com.zebra.sdk.common.card.errors.ZebraCardErrors;
import com.zebra.sdk.common.card.exceptions.ZebraCardException;
import com.zebra.sdk.common.card.jobSettings.ZebraCardJobSettingNames;
import com.zebra.sdk.common.card.printer.ZebraCardPrinter;
import com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory;
import com.zebra.sdk.printer.ZebraPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredPrinter;
import com.zebra.sdk.printer.discovery.DiscoveredUsbPrinter;
import com.zebra.sdk.printer.discovery.UsbDiscoverer;
import com.zebra.sdk.settings.SettingsException;
import com.zebra.sdk.zxp.comm.internal.ZXPBase.Response;
import com.zebra.sdk.zxp.comm.internal.ZXPPrn;
import com.zebra.sdk.zxp.device.internal.ZxpDevice;

public class ZebraPrinterService {
	
	public final Integer RESUME_OPTION = 0;
	private final Integer CARD_FEED_TIMEOUT = 30000;
	
	Logger log = Logger.getLogger(EsupSGCClientApplication.class);
	
	DiscoveredUsbPrinter discoveredPrinters;
	ZebraCardPrinterFactory zebraCardPrinterFactory;
	ZebraCardPrinter zebraCardPrinter;
	ZebraPrinter zebraPrinter;
	ZxpDevice zxpDevice;
	ZXPPrn zxpPrn;
	Connection connection;
	int jobId;
	
	public ZebraPrinterService() throws ConnectionException {

		DiscoveredUsbPrinter[] discoveredPrinters;
		try {
			discoveredPrinters = UsbDiscoverer.getZebraUsbPrinters();
			for(DiscoveredUsbPrinter discoveredUsbPrinter : discoveredPrinters) {
				DiscoveredPrinter discoveredPrinter =  discoveredUsbPrinter;
				connection = discoveredPrinter.getConnection();
				
				try {
					if (!connection.isConnected()) connection.open();
					Utils.sleep(1000);
					zebraCardPrinter = ZebraCardPrinterFactory.getInstance(connection);
					zxpDevice = new ZxpDevice(connection);
					zxpPrn = zxpDevice.getZxpPrinter();
					break;
				}catch(Exception e) {
					log.error(e.getMessage());
					throw new ConnectionException(e);
				}
			}
			if(zebraCardPrinter != null ) {
					setSmartcardJob();
			}
		} catch (ConnectionException e) {
			log.error("zebra connection error", e);
		}
		

	}
	public void setSmartcardJob(){
		try {
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_SOURCE, "Feeder");
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.CARD_DESTINATION, "Hold");
			zebraCardPrinter.setJobSetting(ZebraCardJobSettingNames.SMART_CARD_CONTACTLESS, "MIFARE");
		} catch (SettingsException e) {
			log.error("Zebra settings error", e);
		}
	}

	public void launchEncoding() {
		try {
			setSmartcardJob();
			jobId = zebraCardPrinter.smartCardEncode(1);
		} catch (SettingsException | ZebraCardException | ConnectionException e) {
			log.error("Zebra card encoder activation error", e);
		}
	}
	
	public boolean pollJobStatus(){
		boolean done = false;
		long start = System.currentTimeMillis();

		log.debug("Polling status for job id " + jobId + "...\n");
		
		while(!done) {
			Utils.sleep(250);
			JobStatusInfo jobStatus = null;
			try {
				jobStatus = zebraCardPrinter.getJobStatus(jobId);
			} catch (ConnectionException | ZebraCardException e) {
				log.error("zebra : get job status",e);
				return false;
			}
	
			String alarmDesc = jobStatus.alarmInfo.value > 0 ? " (" + jobStatus.alarmInfo.description + ")" : "";
			String errorDesc = jobStatus.errorInfo.value > 0 ? " (" + jobStatus.errorInfo.description + ")" : "";
	
			log.debug(String.format("Job %d: status:%s, position:%s, contact:%s, contactless:%s, alarm:%d%s, error:%d%s%n", jobId, jobStatus.printStatus, jobStatus.cardPosition,
					jobStatus.contactSmartCard, jobStatus.contactlessSmartCard, jobStatus.alarmInfo.value, alarmDesc, jobStatus.errorInfo.value, errorDesc));
			if (jobStatus.printStatus.contains("done_ok") || jobStatus.contactlessSmartCard.contains("at_station")) {
				done = true;
			} else if (jobStatus.printStatus.contains("cancelled_by_user") ) {
				try {
					cancelJobs();
				} catch (ConnectionException | ZebraCardException e) {
					log.debug("cancel job failed");
				}
				return false;
			} else if (jobStatus.printStatus.contains("error") || jobStatus.printStatus.contains("cancelled")) {
				log.debug("Zebra job error");
			} else if (jobStatus.errorInfo.value > 0) {
				log.debug("The job encountered an error [" + jobStatus.errorInfo.description + "] and was cancelled.");
				break;
			} else if (jobStatus.alarmInfo.value > 0) {
				log.debug("Zebra alarm : " + jobStatus.alarmInfo.value);
			} else if ((jobStatus.printStatus.contains("in_progress") && jobStatus.cardPosition.contains("feeding")) // ZMotif printers
					|| (jobStatus.printStatus.contains("alarm_handling") && jobStatus.alarmInfo.value == ZebraCardErrors.MEDIA_OUT_OF_CARDS)) { // ZXP printers
				if (System.currentTimeMillis() > start + CARD_FEED_TIMEOUT) {
					log.warn("Card feed time out");
				}
			}
		}
		return done;
	}
	
	public void cancelJob(){
		try {
			zebraCardPrinter.cancel(jobId);
			log.info("Job ID " + jobId + " was cancelled.%n");
		} catch (ConnectionException | ZebraCardException e) {
			log.error(e);
		}
		
	}
	
	public void cancelJobs() throws ConnectionException, ZebraCardException {
			List<JobStatus> jobs = zebraCardPrinter.getJobList();
			for(JobStatus job : jobs) {
				zebraCardPrinter.cancel(job.jobId);
			}

	}
	
	public void resume(){
		try {
			zebraCardPrinter.resume();
		} catch (ZebraCardException | ConnectionException e) {
			log.error("Zebra resume error", e);
		}
	}

	public void reverseCard(){
		log.info("try to reverse card");
		try {
			zxpPrn.flipCard(new Response(), new CardError());
		} catch (ConnectionException e) {
			log.error("Zebra resume error", e);
		}
	}
	
	public void reset() {
		try {
			zebraCardPrinter.reset();
		} catch (ZebraCardException | ConnectionException e) {
			log.error("Zebra resume error", e);
		}
	}
	
	public String getStatus() throws ConnectionException, SettingsException, ZebraCardException {
		String status = null;
		while(status == null){
			Utils.sleep(250);
			log.debug(zebraCardPrinter.getPrinterStatus().status +" : " +  zebraCardPrinter.getPrinterStatus().alarmInfo.description);
			status = zebraCardPrinter.getPrinterStatus().status + " " + zebraCardPrinter.getPrinterStatus().alarmInfo.description;
		}
		return status;
	}
	
	public ZebraCardPrinter getZebraCardPrinter() {
		return zebraCardPrinter;
	}

	public int getJobId() {
		return jobId;
	}
	
	public String getStatusMessage(String status) {
		if(status.toLowerCase().contains("out of cards")) {
			return "Chargeur vide";
		}else if(status.toLowerCase().contains("jam")) {
			return "Erreur mécanique";
		}else {
			return "En attente de redémarrage";
		}
		
	}
}
