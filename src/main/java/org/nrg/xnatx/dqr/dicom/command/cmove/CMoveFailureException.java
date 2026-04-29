/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cmove.CMoveFailureException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cmove;

import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;

public class CMoveFailureException extends DqrRuntimeException {

    private static final long serialVersionUID = 1L;

    public CMoveFailureException(String arg0) {
        super(arg0);
    }

    public CMoveFailureException(String message, Throwable cause) {
        super(message, cause);
    }
}
