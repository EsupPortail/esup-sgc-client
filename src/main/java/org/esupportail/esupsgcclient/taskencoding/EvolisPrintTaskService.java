package org.esupportail.esupsgcclient.taskencoding;

import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;

public class EvolisPrintTaskService extends EsupSgcTaskService<Void> {

	private final static Logger log = Logger.getLogger(EvolisPrintTaskService.class);

	String qrcode;
	String bmpColorAsBase64;

	String bmpBlackAsBase64;

	public EvolisPrintTaskService(String qrcode, String bmpColorAsBase64, String bmpBlackAsBase64) {
		super();
		this.qrcode = qrcode;
		this.bmpColorAsBase64 = bmpColorAsBase64;
		this.bmpBlackAsBase64 = bmpBlackAsBase64;
	}

	@Override
	protected Task<Void> createTask() {
		Task<Void> evolisTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateTitle("Impression de la carte");
				EvolisPrinterService.print(bmpColorAsBase64, bmpBlackAsBase64, "todo");
				updateTitle("Impression de la carte OK");
				return null;
			}
		};
		return evolisTask;
	}

	@Override
	public EsupSgcTaskService getNext() {
		return new EncodingTaskService(qrcode, true);
	}

}
