package org.esupportail.esupsgcclient.service.pcsc;

import org.esupportail.esupsgcclient.utils.Utils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import javax.smartcardio.*;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

@Service
public class PcscUsbService {

	private final Logger log = LoggerFactory.getLogger(PcscUsbService.class);

	private Card card;
	private CardTerminal cardTerminal;
	private TerminalFactory context;
	private CardTerminals terminals;
	
	void init() throws PcscException {
		try {
			context = TerminalFactory.getInstance("PC/SC", null);
			terminals = context.terminals();
		} catch (Exception e) {
			throw new PcscException("Exception retrieving context", e);
		}
	}
	
	public boolean waitForCardAbsent(long timeout) throws CardException{
		return cardTerminal.waitForCardAbsent(timeout);
	}

	public boolean waitForCardPresent(long timeout) throws CardException{
		return cardTerminal.waitForCardPresent(timeout);
	}

	public boolean isCardPresent() throws CardException{
		return cardTerminal.isCardPresent();
	}

	public String connection() throws CardException, PcscException {
		for (CardTerminal terminal : terminals.list()) {
			if(terminal.isCardPresent()){
				log.info("Try with terminal " + terminal.getName());
				try{
					card = terminal.connect("*");
					cardTerminal = terminal;
					return cardTerminal.getName();
				} catch(Exception e) {
					// if card nfc is ko for example
					if(e.getMessage().contains("SCARD_E_NO_SMARTCARD") || e.getMessage().contains("SCARD_W_UNPOWERED_CARD") || e.getMessage().contains("SCARD_W_UNRESPONSIVE_CARD")) {
						log.warn("wait ... " + e.getMessage());
						log.info("wait ... " + terminal.getName());
						Utils.sleep(500);
						init();
						return connection();
					} else {
						throw new RuntimeException("pcsc connection error - " + e.getMessage(), e);
					}
				}
			}
		}
		throw new CardException("No NFC reader found with card on it - NFC reader found : " + getNamesOfTerminals(terminals));
	}

	private String getNamesOfTerminals(CardTerminals terminals) throws CardException {
		List<String> terminalsNames = new ArrayList<String>();  
		for (CardTerminal terminal : terminals.list()) {
			terminalsNames.add(terminal.getName());
		}
		return terminalsNames.toString();
	}

	public String getTerminalName() throws CardException, PcscException {
		try {
			if (terminals == null || terminals.list().isEmpty()) {
				init();
			}
		} catch(Exception e) {
			log.warn("Exception on PC/SC - retry to init", e);
			init();
		}
		for (CardTerminal terminal : terminals.list()) {
			if(!terminal.getName().contains("6121")) return terminal.getName();
		}
		return null;
	}
	
	public String sendAPDU(String apdu) throws CardException{
		ResponseAPDU answer = null;
		answer = card.getBasicChannel().transmit(new CommandAPDU(hexStringToByteArray(apdu)));
		return byteArrayToHexString(answer.getBytes());

	}

	public String getCardId() throws CardException{
		ResponseAPDU answer;
		answer = card.getBasicChannel().transmit(new CommandAPDU(hexStringToByteArray("FFCA000000")));
		byte[] uid = Arrays.copyOfRange(answer.getBytes(), 0, answer.getBytes().length-2);
		return byteArrayToHexString(uid);
	}
	
	public void disconnect() throws PcscException{
		try {
			card.disconnect(false);
		} catch (CardException e) {
			throw new PcscException("pcsc disconnect error : ", e);
		}
	}

	public byte[] hexStringToByteArray(String s) {
		s = s.replace(" ", "");
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	final private char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

	public String byteArrayToHexString(byte[] bytes) {
		char[] hexChars = new char[bytes.length*2];
		int v;

		for(int j=0; j < bytes.length; j++) {
			v = bytes[j] & 0xFF;
			hexChars[j*2] = hexArray[v>>>4];
			hexChars[j*2 + 1] = hexArray[v & 0x0F];
		}

		return new String(hexChars);
	}
	
}
