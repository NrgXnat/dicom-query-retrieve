package org.nrg.xnatx.dqr.exceptions;

import org.nrg.xapi.exceptions.DataFormatException;

public class CsvPacsRequestTooManyRowsException extends DataFormatException {

    private static final long serialVersionUID = 7339285508180243325L;

    public CsvPacsRequestTooManyRowsException(final String message) {
        super(message);
    }

}
