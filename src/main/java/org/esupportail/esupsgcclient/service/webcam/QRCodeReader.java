package org.esupportail.esupsgcclient.service.webcam;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

import org.apache.log4j.Logger;
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
public class QRCodeReader {

	private final static Logger log = Logger.getLogger(QRCodeReader.class);

	public String readQrCode(BufferedImage esupSGCClientJFrame) {
		Result result = null;
		LuminanceSource source = new BufferedImageLuminanceSource(esupSGCClientJFrame);
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
			return null;
		}
	}

}