/*
 * CMoveFailureException
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dicom.command.cmove;

import org.nrg.dqr.util.DqrRuntimeException;

public class CMoveFailureException extends DqrRuntimeException {

    private static final long serialVersionUID = 1L;

    public CMoveFailureException(String arg0) {
        super(arg0);
    }
}
