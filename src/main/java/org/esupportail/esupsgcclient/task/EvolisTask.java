package org.esupportail.esupsgcclient.task;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;

public class EvolisTask extends Task<Void> {

	private final static Logger log = Logger.getLogger(EvolisTask.class);

	String bmpColorAsBase64;

	String bmpBlackAsBase64;

	public EvolisTask(String bmpColorAsBase64, String bmpBlackAsBase64) {
		this.bmpColorAsBase64 = bmpColorAsBase64;
		this.bmpBlackAsBase64 = bmpBlackAsBase64;
	}

	@Override
	protected Void call() throws Exception {
		try {
			EvolisPrinterService.print(bmpColorAsBase64, bmpBlackAsBase64, "todo");
		} catch(Exception e) {
			throw new RuntimeException("Error printing card : " + e.getMessage(), e);
		}
		return null;
	}

}
