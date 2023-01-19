package org.esupportail.esupsgcclient.service.printer.evolis;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class EvolisResponse {
    final static Logger log = LoggerFactory.getLogger(EvolisResponse.class);
    static ObjectMapper objectMapper = new ObjectMapper();
    String jsonrpc;

    String id;

    String result;

    EvolisError error;


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

    public String getResult() {
        return result;
    }

    public void setResult(String result) {
        this.result = result;
    }

    public EvolisError getError() {
        return error;
    }

    public void setError(EvolisError error) {
        this.error = error;
    }

    @Override
    public String toString() {
        try {
            String cmdString = objectMapper.writeValueAsString(this);
            return cmdString.length()>200 ? cmdString.substring(0,200) + "..." + cmdString.substring(cmdString.length()-199) : cmdString;
        } catch (JsonProcessingException e) {
            log.info("Exception on EvolisResponse.toString via json mapper",  e);
        }
        return super.toString();
    }

}
