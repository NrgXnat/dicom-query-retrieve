/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.strategy.orm.bjc.BjcResultSetLimitStrategy
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm.bjc;

import org.nrg.xnatx.dqr.dicom.strategy.orm.BasicResultSetLimitStrategy;

public class BjcResultSetLimitStrategy extends BasicResultSetLimitStrategy {

    @Override
    protected int minimumLengthOfPatientNameAsTheSoleSearchCriterion() {
        return 2;
    }
}
