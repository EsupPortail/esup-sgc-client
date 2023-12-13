package org.esupportail.esupsgcclient.hack.zebra;

import com.zebra.sdk.comm.ConnectionException;
import org.apache.log4j.Logger;

/*
Without this hack, we get this error on our ZXP3 printer :
    * lcd screen : command error
    * job status : error:3005 (ZMC: Invalid parameter (1))

  The error is due to the "null" parameter in the command (for example) :
    [ESC]HALF 567 null
    [ESC]E 2

  This Hack replace the "null" parameter by "0" :
    [ESC]HALF 567 0
    [ESC]E 2

    We suspect that the error occurs only when the printer uses half panel ribbon.

  at com.zebra.sdk.comm.UsbConnection.write
  ...
  at com.zebra.sdk.common.card.comm.internal.DeviceStream.write(Unknown Source:-1)
  at com.zebra.sdk.zxp.comm.internal.ZXPBase.sendCmd(Unknown Source:-1)
  at com.zebra.sdk.zxp.comm.internal.ZXPBase.sendCmdAndReadBuffer(Unknown Source:-1)
  at com.zebra.sdk.zxp.comm.internal.ZXPBase.sendCmdAndReadBuffer(Unknown Source:-1)
  at com.zebra.sdk.zxp.comm.internal.ZXPBase.setHalfPanelOffsets(Unknown Source:-1)
  at com.zebra.sdk.zxp.comm.internal.ZXPPrn.setHalfPanelOffset(Unknown Source:-1)
  at com.zebra.sdk.zxp.job.internal.ProcessJob.sendHalfPanelCommand(Unknown Source:-1)
  at com.zebra.sdk.zxp.job.internal.ProcessJob.processJob(Unknown Source:-1)
  at com.zebra.sdk.zxp.job.internal.ProcessJob$1.run(Unknown Source:-1)


  Note : because of ZebraCardPrinterFactoryHelper.USB_CONNECTION_CLASS_NAME = "UsbConnection"
  This class must be named "UsbConnection" - if named with another name, the printer is not initialized with this error :

  com.zebra.sdk.comm.ConnectionException: Unable to determine printer type
	at com.zebra.sdk.common.card.printer.internal.ZebraCardPrinterFactoryHelper.getPrinterType(Unknown Source)
	at com.zebra.sdk.common.card.printer.internal.ZebraCardPrinterFactoryHelper.getInstance(Unknown Source)
	at com.zebra.sdk.common.card.printer.ZebraCardPrinterFactory.getInstance(Unknown Source)

 */
public class UsbConnection extends com.zebra.sdk.comm.UsbConnection {

    private final static Logger log = Logger.getLogger(UsbConnection.class);

    public UsbConnection(String var1) throws ConnectionException {
        super(var1);
    }

    @Override
    public void write(byte[] bytes, int i, int i1) throws ConnectionException {
        String s = new String(bytes, i, i1);
        if(s.matches("(?s).*HALF [0-9]+ null.*")) {
            String olds = s;
            s = s.replaceFirst("(HALF [0-9]+) null", "$1 0");
            log.warn("Bad Half Command caught - replaced : " + formatMsg4Log(olds) + " with " + formatMsg4Log(s));
            super.write(s.getBytes());
        } else {
            if(s.contains(" null")) {
                log.info("Command with null caught : " + formatMsg4Log(s));
            }
            if(log.isTraceEnabled()) {
                log.trace("Command sent : " + formatMsg4Log(s));
            }
            super.write(bytes, i, i1);
        }
    }

    protected String formatMsg4Log(String input) {
        String output = input.replaceAll("\u001B", "[ESC]");
        output = output.replaceAll("\r\n", "[CRLF]"); // Pour Windows
        output = output.replaceAll("\n", "[LF]"); // Pour Linux/Unix
        output = output.replaceAll("\r", "[CR]"); // Pour Mac
        return output;
    }
}
