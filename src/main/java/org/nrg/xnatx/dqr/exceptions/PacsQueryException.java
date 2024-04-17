package org.nrg.xnatx.dqr.exceptions;

public class PacsQueryException extends PacsException {
    protected PacsQueryException(String message) {
        super(message);
    }

    protected PacsQueryException(Exception cause) {
        super(cause);
    }

    public PacsQueryException(String message, Exception cause) {
        super(message, cause);
    }
}
