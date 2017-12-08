/*
 * BjcResultSetLimitStrategy
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dicom.strategy.orm.bjc;

import org.nrg.dqr.dicom.strategy.orm.BasicResultSetLimitStrategy;

public class BjcResultSetLimitStrategy extends BasicResultSetLimitStrategy {

    @Override
    protected int minimumLengthOfPatientNameAsTheSoleSearchCriterion() {
        return 2;
    }
}
