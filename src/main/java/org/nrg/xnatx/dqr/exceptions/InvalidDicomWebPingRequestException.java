package org.nrg.xnatx.dqr.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidDicomWebPingRequestException extends Exception {
    public InvalidDicomWebPingRequestException(final String message, final Exception cause) {
        super(message, cause);
    }

    public InvalidDicomWebPingRequestException(final String message) {
        super(message);
    }
}
