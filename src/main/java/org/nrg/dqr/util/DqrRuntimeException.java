/*
 * org.nrg.dqr.util.DqrRuntimeException
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.util;

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
