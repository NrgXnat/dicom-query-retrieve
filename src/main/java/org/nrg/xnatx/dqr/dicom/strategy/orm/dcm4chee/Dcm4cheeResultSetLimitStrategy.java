/*
 * Dcm4cheeResultSetLimitStrategy
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm.dcm4chee;

import org.nrg.xnatx.dqr.dicom.strategy.orm.BasicResultSetLimitStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;

public class Dcm4cheeResultSetLimitStrategy extends BasicResultSetLimitStrategy {
    @Override
    public boolean searchCriteriaIsSufficientlySpecific(final PacsSearchCriteria searchCriteria) {
        // with our own PACS we don't care about overloading it, allow "SELECT * FROM PACS"-style searches
        return true;
    }
}
