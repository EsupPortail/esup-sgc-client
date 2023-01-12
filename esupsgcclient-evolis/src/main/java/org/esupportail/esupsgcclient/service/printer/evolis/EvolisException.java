package org.esupportail.esupsgcclient.service.printer.evolis;

public class EvolisException extends RuntimeException {

    EvolisError error;
    public EvolisException(EvolisError error) {
        this.error = error;
    }

    @Override
    public String getMessage() {
        return error.getMessage();
    }
}
