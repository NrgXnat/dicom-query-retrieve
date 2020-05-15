/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cmove.CMoveTargetNotFoundException
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
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
