/*
 * CMoveTargetNotFoundException
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.command.cmove;

import org.nrg.xnatx.dqr.utils.DqrRuntimeException;

public class CMoveTargetNotFoundException extends DqrRuntimeException {

    private static final long serialVersionUID = 1L;

    public CMoveTargetNotFoundException() {
    }

    public CMoveTargetNotFoundException(String arg0) {
        super(arg0);
    }
}
