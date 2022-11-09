package org.esupportail.esupsgcclient.service.printer.evolis;

import java.util.HashMap;
import java.util.Map;

public class EvolisResponse {

    String jsonrpc;

    String id;

    String result;


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

}
