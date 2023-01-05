package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.Socket;


/**
 * This Service provides commands for interaction with sgc from evolis printers
 * It computes JSON-RPC commands to send to Evolis Services Provider 2
 */
public class EvolisPrinterCommands {

	static String JOB_ID = "JOB000001";

	static EvolisRequest getPrinterStatus() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("SUPERVISION.List");
		req.getParams().put("device", "Evolis Primacy 2");
		req.getParams().put("level", "2");
		return req;
	}

	static EvolisRequest insertCard() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Ss");
		req.getParams().put("device", "Evolis Primacy 2");
		req.getParams().put("timeout", "5000");
		return req;
	}

	static EvolisRequest printBegin() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.Begin");
		req.getParams().put("device", "Evolis Primacy 2");
		req.getParams().put("session", JOB_ID);
		return req;
	}

	static EvolisRequest printSet(String printerEvolisSet) {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.Set");
		req.getParams().put("data", printerEvolisSet);
		req.getParams().put("session", JOB_ID);
		return req;
	}

	static EvolisRequest printFrontColorBmp(String bmpAsBase64) {
		return printColorBmp(bmpAsBase64, "color");
	}

	static EvolisRequest printFrontBlackBmp(String bmpAsBase64) {
		return printColorBmp(bmpAsBase64, "resin");
	}

	/*
		bmpAsBase64 : just black image here for varnish ?
	 */
	static EvolisRequest printFrontVarnish(String bmpAsBase64) {
		return printColorBmp(bmpAsBase64, "varnish");
	}

	private static EvolisRequest printColorBmp(String bmpAsBase64, String panel) {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.SetBitmap");
		req.getParams().put("session", JOB_ID);
		req.getParams().put("face", "front");
		req.getParams().put("panel", panel);
		req.getParams().put("data", "base64:" + bmpAsBase64);
		req.getParams().put("timeout", "5000");
		return req;
	}
	static EvolisRequest print() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.Print");
		req.getParams().put("session", JOB_ID);
		req.getParams().put("timeout", "20000");
		return req;
	}

	static EvolisRequest printEnd() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.End");
		req.getParams().put("session", JOB_ID);
		return req;
	}

	static EvolisRequest insertCardToContactLessStation() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Sic");
		req.getParams().put("device", "Evolis Primacy 2");
		req.getParams().put("timeout", "5000");
		return req;
	}

	static EvolisRequest eject() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Se");
		req.getParams().put("device", "Evolis Primacy 2");
		req.getParams().put("timeout", "5000");
		return req;
	}

	static EvolisRequest reject() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Ser");
		req.getParams().put("device", "Evolis Primacy 2");
		req.getParams().put("timeout", "5000");
		return req;
	}

	public static EvolisRequest startSequence() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Ss");
		return req;
	}

	public static EvolisRequest endSequence() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Se");
		return req;
	}

	public static EvolisRequest getNextCleaningSteps() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Rcsc;other");
		req.getParams().put("device", "Evolis Primacy 2");
		req.getParams().put("timeout", "5000");
		return req;
	}

}
