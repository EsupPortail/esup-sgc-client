package org.esupportail.esupsgcclient.service.webcam;

import com.github.sarxos.webcam.WebcamException;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.BinaryBitmap;
import com.google.zxing.DecodeHintType;
import com.google.zxing.LuminanceSource;
import com.google.zxing.MultiFormatReader;
import com.google.zxing.NotFoundException;
import com.google.zxing.Result;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import javafx.embed.swing.SwingFXUtils;
import org.apache.log4j.Logger;
import org.esupportail.esupsgcclient.tasks.EsupSgcTask;
import org.esupportail.esupsgcclient.tasks.simple.QrCodeTask;
import org.esupportail.esupsgcclient.utils.Utils;
import org.springframework.stereotype.Service;

import java.awt.image.BufferedImage;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.FutureTask;

@Service 
public class QRCodeReader {

	private final static Logger log = Logger.getLogger(QRCodeReader.class);

	String readQrCode(BufferedImage esupSGCClientJFrame) {
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

	public String getQrcode(EsupSgcTask esupSgcTask, int nbRetryMax) {
		String qrcode = null;
		long nbRetry = 0;
		while (true && nbRetryMax>0 && nbRetry<nbRetryMax) {
			if(esupSgcTask.isCancelled()) {
				return null;
			}
			BufferedImage webcamBufferedImage = SwingFXUtils.fromFXImage(esupSgcTask.webcamImageProperty.get(), null);
			qrcode = readQrCode(webcamBufferedImage);
			if(webcamBufferedImage != null) {
				if (qrcode != null) {
					break;
				}
			} else {
				throw new WebcamException("no image");
			}
			nbRetry++;
			Utils.sleep(200);
		}
		return qrcode;
	}
}