package org.esupportail.esupsgcclient.service.webcam;


import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.ui.EsupSGCJFrame;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
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
		while(result == null) {
			BufferedImage image = null;
			esupSGCJFrame.webCamPanel.resume();
			if (esupSGCJFrame.webcam.isOpen() && (image=esupSGCJFrame.webcam.getImage()) != null) {
				image = esupSGCJFrame.webcam.getImage();
				LuminanceSource source = new BufferedImageLuminanceSource(image);
				BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));
				try {
					MultiFormatReader qrReader = new MultiFormatReader();
					Map<DecodeHintType, Object> hints = new EnumMap<DecodeHintType, Object>(DecodeHintType.class);
					hints.put(DecodeHintType.CHARACTER_SET, "UTF-8");
					hints.put(DecodeHintType.TRY_HARDER, true);
					List<BarcodeFormat> barcodeFormats = new ArrayList<BarcodeFormat>();
					barcodeFormats.add(BarcodeFormat.QR_CODE);
					hints.put(DecodeHintType.POSSIBLE_FORMATS, barcodeFormats);
					qrReader.setHints(hints);
					result = qrReader.decode(bitmap);
					return result.getText();
				} catch (NotFoundException e) {
					log.trace("QRCode not found");
				}
			}
			esupSGCJFrame.webCamPanel.pause();
			Utils.sleep(250);
		}
		return null;
	}
	
}