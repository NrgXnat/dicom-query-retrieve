/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.DqrRuntimeException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.exceptions;

public class DqrRuntimeException extends RuntimeException {
    private static final long serialVersionUID = -8897332425876424774L;

    public DqrRuntimeException() {
    }

    public DqrRuntimeException(final String message) {
        super(message);
    }

    public DqrRuntimeException(final Throwable throwable) {
        super(throwable);
    }

    public DqrRuntimeException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
