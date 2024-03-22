package org.nrg.xnatx.dqr.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

import java.util.List;

@ResponseStatus(value = HttpStatus.BAD_REQUEST)
public class InvalidPacsConfigurationException extends Exception {
    public InvalidPacsConfigurationException(final List<String> errors) {
        this(String.join(", ", errors));
    }

    public InvalidPacsConfigurationException(final String message) {
        super(message);
    }
}
