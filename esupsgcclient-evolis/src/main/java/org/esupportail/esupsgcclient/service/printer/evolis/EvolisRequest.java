package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public class EvolisRequest {

    final static Logger log = LoggerFactory.getLogger(EvolisRequest.class);
    static ObjectMapper objectMapper = new ObjectMapper();
    String jsonrpc = "2.0";

    String id = "1";

    String method = "CMD.SendCommand";

	Map<String, String> params = new HashMap<>();

    public String getJsonrpc() {
        return jsonrpc;
    }

    public void setJsonrpc(String jsonrpc) {
        this.jsonrpc = jsonrpc;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getMethod() {
        return method;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public Map<String, String> getParams() {
        return params;
    }

    public void setParams(Map<String, String> params) {
        this.params = params;
    }

    @Override
    public String toString() {
        try {
            String cmdString = objectMapper.writeValueAsString(this);
            return cmdString.length()>200 ? cmdString.substring(0,200) + "..." + cmdString.substring(cmdString.length()-199) : cmdString;
        } catch (JsonProcessingException e) {
           log.info("Exception on EvolisRequest.toString via json mapper",  e);
        }
        return super.toString();
    }
}
