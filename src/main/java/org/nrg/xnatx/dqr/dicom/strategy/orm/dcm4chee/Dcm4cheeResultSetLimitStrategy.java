/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.strategy.orm.dcm4chee.Dcm4cheeResultSetLimitStrategy
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
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
