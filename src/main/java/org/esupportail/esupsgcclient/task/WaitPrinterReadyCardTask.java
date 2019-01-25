package org.esupportail.esupsgcclient.task;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.ZebraPrinterService;
import org.esupportail.esupsgcclient.utils.Utils;

import javafx.concurrent.Task;

public class WaitPrinterReadyCardTask extends Task<Void> {

	private final static Logger log = Logger.getLogger(WaitPrinterReadyCardTask.class);
	
	@Override
    protected Void call() throws Exception {
		ZebraPrinterService.cancelJobs();
		String startStatus = ZebraPrinterService.getStatus();
		while (!startStatus.contains("idle") && !startStatus.contains("ribbon")) {
			startStatus = ZebraPrinterService.getStatus();
			log.warn("Imprimante indisponible : " + ZebraPrinterService.getStatusMessage(startStatus));
			Utils.sleep(1000);
		}
		return null;
    }

}
