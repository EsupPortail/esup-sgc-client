package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.databind.ObjectMapper;
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
		req.getParams().put("session", "JOB000001");
		return req;
	}

	static EvolisRequest printSet() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.Set");
		req.getParams().put("data", "GRibbonType=RC_YMCKO;FOverlayManagement=FULLVARNISH");
		req.getParams().put("session", "JOB000001");
		return req;
	}

	static EvolisRequest printFrontColorBmp(String bmpAsBase64) {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.SetBitMap");
		req.getParams().put("session", "JOB000001");
		req.getParams().put("face", "front");
		req.getParams().put("panel", "color");
		req.getParams().put("data", "base64:" + bmpAsBase64);
		return req;
	}

	static EvolisRequest printFrontBlackBmp(String bmpAsBase64) {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.SetBitMap");
		req.getParams().put("session", "JOB000001");
		req.getParams().put("face", "front");
		req.getParams().put("panel", "resin");
		req.getParams().put("data", "base64:" + bmpAsBase64);
		return req;
	}

	/*
		bmpAsBase64 : just black image here for varnish ?
	 */
	static EvolisRequest printFrontVarnish(String bmpAsBase64) {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.SetBitMap");
		req.getParams().put("session", "JOB000001");
		req.getParams().put("face", "front");
		req.getParams().put("panel", "varnish");
		req.getParams().put("data", "base64:" + bmpAsBase64);
		return req;
	}

	static EvolisRequest print() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.Print");
		req.getParams().put("session", "JOB000001");
		return req;
	}

	static EvolisRequest printEnd() {
		EvolisRequest req = new EvolisRequest();
		req.setMethod("PRINT.End");
		req.getParams().put("session", "JOB000001");
		return req;
	}

	static EvolisRequest insertCardToContactLessStation() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Sic");
		return req;
	}

	static EvolisRequest eject() {
		EvolisRequest req = new EvolisRequest();
		req.getParams().put("command", "Se");
		return req;
	}

}
