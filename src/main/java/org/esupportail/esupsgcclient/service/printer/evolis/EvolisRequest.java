package org.esupportail.esupsgcclient.service.printer.evolis;

import java.util.HashMap;
import java.util.Map;

public class EvolisRequest {

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
}
