package org.esupportail.esupsgcclient.task;

import javafx.concurrent.Service;
import javafx.concurrent.Task;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.evolis.EvolisPrinterService;

public class EvolisEjectTaskService extends Service<Void> {

	private final static Logger log = Logger.getLogger(EvolisEjectTaskService.class);

	final boolean eject4success;

	public EvolisEjectTaskService(boolean eject4success) {
		super();
		this.eject4success = eject4success;
	}

	@Override
	protected Task<Void> createTask() {
		Task<Void> evolisTask = new Task<Void>() {
			@Override
			protected Void call() throws Exception {
				updateTitle("Ejection de la carte");
				if(eject4success) {
					EvolisPrinterService.eject();
				} else {
					EvolisPrinterService.reject();
				}
				return null;
			}
		};
		return evolisTask;
	}

}
