package org.esupportail.esupsgcclient.service.webcam;


import java.awt.image.BufferedImage;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.ui.EsupSGCJFrame;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Service;

import com.google.zxing.BinaryBitmap;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;

@Service
public class WebcamQRCodeReader {

	private final static Logger log = Logger.getLogger(WebcamQRCodeReader.class);
	
	private EsupSGCJFrame esupSGCJFrame = null;
	private String eppnInit;

	public String getEppnInit() {
		return eppnInit;
	}

	public void setEppnInit(String eppnInit) {
		this.eppnInit = eppnInit;
	}

	public WebcamQRCodeReader(String eppnInit, String sgcUrl, EsupSGCJFrame esupSGCClientJFrame) {
		setEppnInit(eppnInit);
		this.esupSGCJFrame = esupSGCClientJFrame;
	}

	public String readQrCode() {
		Result result = null;
		BufferedImage image = null;
		while(result == null) {
			esupSGCJFrame.panel.resume();
			if (esupSGCJFrame.webcam.isOpen() && (image=esupSGCJFrame.webcam.getImage()) != null) {
				image = esupSGCJFrame.webcam.getImage();
				LuminanceSource source = new BufferedImageLuminanceSource(image);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
				try {
					result = new MultiFormatReader().decode(bitmap);
					return result.getText();
				} catch (NotFoundException e) {
					esupSGCJFrame.addLogTextLn("no QR code found in image");
					esupSGCJFrame.initSteps();
				}
			}
			esupSGCJFrame.panel.pause();
			//tempo cpu usage ?
			Utils.sleep(200);
		}
		log.error("QRCode read error");
		return null;
	}
	
}