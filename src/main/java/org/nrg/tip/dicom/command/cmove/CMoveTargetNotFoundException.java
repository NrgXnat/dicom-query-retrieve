/*
 * org.nrg.tip.dicom.command.cmove.CMoveTargetNotFoundException
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.command.cmove;

import org.nrg.tip.util.TipRuntimeException;

public class CMoveTargetNotFoundException extends TipRuntimeException {

    private static final long serialVersionUID = 1L;

    public CMoveTargetNotFoundException() {
    }

    public CMoveTargetNotFoundException(String arg0) {
        super(arg0);
    }
}
