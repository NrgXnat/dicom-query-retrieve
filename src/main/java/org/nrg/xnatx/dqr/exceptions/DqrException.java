/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.exceptions.DqrRuntimeException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.exceptions;

public class DqrException extends Exception {
    private static final long serialVersionUID = -1833014309397235936L;

    public DqrException() {
        super();
    }

    public DqrException(final String message) {
        super(message);
    }

    public DqrException(final Throwable throwable) {
        super(throwable);
    }

    public DqrException(final String message, final Throwable throwable) {
        super(message, throwable);
    }
}
