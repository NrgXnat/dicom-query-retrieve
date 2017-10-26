/*
 * org.nrg.tip.util.TipRuntimeException
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.util;

public class TipRuntimeException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    public TipRuntimeException() {
    }

    public TipRuntimeException(String arg0) {
        super(arg0);
    }

    public TipRuntimeException(Throwable arg0) {
        super(arg0);
    }

    public TipRuntimeException(String arg0, Throwable arg1) {
        super(arg0, arg1);
    }
}
