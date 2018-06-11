package org.esupportail.esupsgcclient.task;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.service.printer.ZebraPrinterService;

import com.zebra.sdk.common.card.exceptions.ZebraCardException;

import javafx.concurrent.Task;

@SuppressWarnings("restriction")
public class ReverseCardTask extends Task<Void> {

	private final static Logger log = Logger.getLogger(ReverseCardTask.class);
	
	@Override
    protected Void call() throws Exception {
		try {
			ZebraPrinterService.reverseCard();
		} catch (ZebraCardException e) {
			log.error("ZebraPrinterService : reverse error");
		}
		return null;
    }

}
