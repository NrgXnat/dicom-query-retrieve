/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.utils.DqrRuntimeException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.utils;

public class DqrRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public DqrRuntimeException() {
    }

    public DqrRuntimeException(String arg0) {
        super(arg0);
    }

    public DqrRuntimeException(Throwable arg0) {
        super(arg0);
    }

    public DqrRuntimeException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
