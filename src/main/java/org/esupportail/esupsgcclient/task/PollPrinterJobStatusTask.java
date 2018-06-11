package org.esupportail.esupsgcclient.task;

import org.esupportail.esupsgcclient.service.printer.ZebraPrinterService;

import javafx.concurrent.Task;

@SuppressWarnings("restriction")
public class PollPrinterJobStatusTask extends Task<Boolean> {

	@Override
    protected Boolean call() throws Exception {
		ZebraPrinterService.launchEncoding();
		return ZebraPrinterService.pollJobStatus();
    }

}
