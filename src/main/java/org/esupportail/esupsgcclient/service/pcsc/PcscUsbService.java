package org.esupportail.esupsgcclient.service.pcsc;

import java.security.Security;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import javax.smartcardio.Card;
import javax.smartcardio.CardException;
import javax.smartcardio.CardTerminal;
import javax.smartcardio.CardTerminals;
import javax.smartcardio.CommandAPDU;
import javax.smartcardio.ResponseAPDU;
import javax.smartcardio.TerminalFactory;

import org.apache.log4j.Logger;

import jnasmartcardio.Smartcardio;
import jnasmartcardio.Smartcardio.JnaPCSCException;

@SuppressWarnings("restriction")
public class PcscUsbService {

	private final static Logger log = Logger.getLogger(PcscUsbService.class);
	
	private static Card card;
	private static CardTerminal cardTerminal;
	private static TerminalFactory context;
	private static CardTerminals terminals;
	
	public static void init() throws PcscException {
		Security.addProvider(new Smartcardio());
		try {
			context = TerminalFactory.getInstance("PC/SC", null, Smartcardio.PROVIDER_NAME);
			terminals = context.terminals();
			if(terminals.list().isEmpty()) {
				throw new PcscException("no PC/SC reader found");
			}
		} catch (Exception e) {
			throw new PcscException("Exception retrieving context", e);
		}
	}
	
	public static boolean isCardOnTerminal() throws CardException{
		return cardTerminal.waitForCardAbsent(2);
	}
	
	public static String connection() throws CardException{
		for (CardTerminal terminal : terminals.list()) {
			if(!terminal.getName().contains("6121") && terminal.isCardPresent()){
				cardTerminal = terminal;
				try{
					card = cardTerminal.connect("*");
					return cardTerminal.getName();
				}catch(JnaPCSCException e){
					log.error("pcsc connection error", e);
				}
			}
		}
		throw new CardException("No NFC reader found with card on it - NFC reader found : " + getNamesOfTerminals(terminals));
	}

	private static String getNamesOfTerminals(CardTerminals terminals) throws CardException {
		List<String> terminalsNames = new ArrayList<String>();  
		for (CardTerminal terminal : terminals.list()) {
			terminalsNames.add(terminal.getName());
		}
		return terminalsNames.toString();
	}

	public boolean isCardPresent() throws CardException{
		for (CardTerminal terminal : terminals.list()) {
		try {
			if(!terminal.getName().contains("6121") && terminal.isCardPresent()) return true; 
		} catch (CardException e) {
			log.warn("Pas de carte");
		}
		}
		return false;
	}
	
	public static String sendAPDU(String apdu) throws CardException{
		ResponseAPDU answer = null;
		answer = card.getBasicChannel().transmit(new CommandAPDU(hexStringToByteArray(apdu)));
		return byteArrayToHexString(answer.getBytes());

	}

	public static String getCardId() throws CardException{
		ResponseAPDU answer;
		answer = card.getBasicChannel().transmit(new CommandAPDU(hexStringToByteArray("FFCA000000")));
		byte[] uid = Arrays.copyOfRange(answer.getBytes(), 0, answer.getBytes().length-2);
		return byteArrayToHexString(uid);
	}
	
	public static void disconnect() throws PcscException{
		try {
			card.disconnect(false);
		} catch (CardException e) {
			throw new PcscException("pcsc disconnect error : ", e);
		}
	}

	public static byte[] hexStringToByteArray(String s) {
		s = s.replace(" ", "");
		int len = s.length();
		byte[] data = new byte[len / 2];
		for (int i = 0; i < len; i += 2) {
			data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
					+ Character.digit(s.charAt(i+1), 16));
		}
		return data;
	}

	final private static char[] hexArray = {'0','1','2','3','4','5','6','7','8','9','A','B','C','D','E','F'};

	public static String byteArrayToHexString(byte[] bytes) {
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
