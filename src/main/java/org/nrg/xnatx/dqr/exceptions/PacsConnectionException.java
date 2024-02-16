package org.nrg.xnatx.dqr.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.INTERNAL_SERVER_ERROR)
public class PacsConnectionException extends PacsException {
    public PacsConnectionException(final String message) {
        super(message);
    }

    public PacsConnectionException(final String message, final Exception cause) {
        super(message, cause);
    }
}
