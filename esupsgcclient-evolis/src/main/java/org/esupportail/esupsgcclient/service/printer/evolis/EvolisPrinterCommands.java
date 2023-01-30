package org.esupportail.esupsgcclient.service.printer.evolis;

import jakarta.annotation.Resource;
import org.esupportail.esupsgcclient.AppConfig;
import org.springframework.stereotype.Component;



/**
 * This Service provides commands for interaction with sgc from evolis printers
 * It computes JSON-RPC commands to send to Evolis Services Provider 2
 */
@Component
public class EvolisPrinterCommands {

	static String JOB_ID = "JOB000001";

	@Resource
	AppConfig appConfig;

	EvolisRequest getPrinterStatus() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("SUPERVISION.List");
		req.getParams().put("device", getDeviceName());
		req.getParams().put("level", "2");
		return req;
	}

	EvolisRequest insertCard() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Ss");
		req.getParams().put("device", getDeviceName());
		req.getParams().put("timeout", "5000");
		return req;
	}

	EvolisRequest printBegin() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.Begin");
		req.getParams().put("device", getDeviceName());
		req.getParams().put("session", JOB_ID);
		return req;
	}

	EvolisRequest printSet(String printerEvolisSet) {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.Set");
		req.getParams().put("data", printerEvolisSet);
		req.getParams().put("session", JOB_ID);
		return req;
	}

	EvolisRequest printFrontColorBmp(String bmpAsBase64) {
		return printColorBmp(bmpAsBase64, "color");
	}

	EvolisRequest printFrontBlackBmp(String bmpAsBase64) {
		return printColorBmp(bmpAsBase64, "resin");
	}

	/*
		bmpAsBase64 : just black image here for varnish ?
	 */
	EvolisRequest printFrontVarnish(String bmpAsBase64) {
		return printColorBmp(bmpAsBase64, "varnish");
	}

	private EvolisRequest printColorBmp(String bmpAsBase64, String panel) {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.SetBitmap");
		req.getParams().put("session", JOB_ID);
		req.getParams().put("face", "front");
		req.getParams().put("panel", panel);
		req.getParams().put("data", "base64:" + bmpAsBase64);
		req.getParams().put("timeout", "5000");
		return req;
	}
	EvolisRequest print() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.Print");
		req.getParams().put("session", JOB_ID);
		req.getParams().put("timeout", "20000");
		return req;
	}

	EvolisRequest printEnd() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.End");
		req.getParams().put("session", JOB_ID);
		req.getParams().put("timeout", "5000");
		return req;
	}

	EvolisRequest insertCardToContactLessStation() {
		return getEvolisCommandFromPlainText("Sic");
	}

	EvolisRequest eject() {
		return getEvolisCommandFromPlainText("Se");
	}

	EvolisRequest reject() {
		return getEvolisCommandFromPlainText("Ser");
	}

	public EvolisRequest startSequence() {
		return getEvolisCommandFromPlainText("Ss");
	}

	public EvolisRequest endSequence() {
		return getEvolisCommandFromPlainText("Se");
	}

	public EvolisRequest noEject() {
		return getEvolisCommandFromPlainText("Psoe;D");
	}

	public EvolisRequest shutdown() {
		return getEvolisCommandFromPlainText("Psdc;Force");
	}

	public EvolisRequest getNextCleaningSteps() {
		EvolisRequest req = new EvolisRequest();
		if(isPrimacy2()) {
			req.getParams().put("command", "Rcsc;other");
		} else {
			req.getParams().put("command", "Rco;rc");
		}
		req.getParams().put("device", getDeviceName());
		req.getParams().put("timeout", "5000");

		return req;
	}

	/*
		Permet d'éviter d'avoir des popups qui interrompent l'édition d'une carte (pour avertir que le ruban est bientôt fini)
	 */
	public EvolisRequest disableFeederNearEmptyPrinterStatus() {
		return getEvolisCommandFromPlainText("Ppsc;n;S;D");
	}

	public EvolisRequest restoreManufactureParameters() {
		return getEvolisCommandFromPlainText("Rmp;all");
	}

	public EvolisRequest evolisRestart() {
		return getEvolisCommandFromPlainText("Srs");
	}

	String getDeviceName() {
		return appConfig.getPrinterDeviceName();
	}

	boolean isPrimacy2() {
		return "Evolis Primacy 2".equals(appConfig.getPrinterDeviceName());
	}

	public EvolisRequest getEvolisCommandFromPlainText(String plainTextCommand) {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", plainTextCommand);
		req.getParams().put("device", getDeviceName());
		req.getParams().put("timeout", "5000");
		return req;
	}

	public EvolisRequest setupCardToContactLessStation() {
		return getEvolisCommandFromPlainText("Poc;=;800");
	}
}
