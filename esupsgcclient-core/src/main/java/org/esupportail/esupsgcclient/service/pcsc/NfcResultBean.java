package org.esupportail.esupsgcclient.service.pcsc;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.io.Serializable;

@JsonIgnoreProperties(ignoreUnknown = true)
public class NfcResultBean implements Serializable {

	private static final long serialVersionUID = 1L;

	public boolean inError() {
		return CODE.ERROR.equals(this.getCode());
	}

	public static enum CODE {
        ERROR,
        OK,
        END
    }

    private CODE code;

    private String cmd;
    
    private String param;
    
    private String size;
    
    private String jSessionId;

    private String fullApdu;
    
    private String msg;
    
    public String getMsg() {
		return msg;
	}

	public void setMsg(String msg) {
		this.msg = msg;
	}

	public String getjSessionId() {
		return jSessionId;
	}

	public void setjSessionId(String jSessionId) {
		this.jSessionId = jSessionId;
	}

	public String getSize() {
		return size;
	}
   
	public void setSize(int size) {
		this.size = String.valueOf(size);
	}

	public String getParam() {
		return param;
	}

	public void setParam(String param) {
		this.param = param;
	}

	public NfcResultBean() {
		super();
	}  
    
    public NfcResultBean(CODE code, String msg) {
		super();
		this.code = code;
		this.fullApdu = msg;
	}

	public CODE getCode() {
		return code;
	}

	public void setCode(CODE code) {
        this.code = code;
    }

    public String getFullApdu() {
        return fullApdu;
    }

    public void setFullApdu(String fullApdu) {
        this.fullApdu = fullApdu;
    }

    public String getCmd() {
		return cmd;
	}

	public void setCmd(String cmd) {
		this.cmd = cmd;
	}

	@Override
	public String toString() {
		return "NfcResultBean [code=" + code + ", cmd=" + cmd + ", param=" + param + ", size=" + size + ", jSessionId="
				+ jSessionId + ", fullApdu=" + fullApdu + ", msg=" + msg + "]";
	}
	
}
