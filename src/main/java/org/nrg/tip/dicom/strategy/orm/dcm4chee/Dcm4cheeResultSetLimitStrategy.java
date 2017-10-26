/*
 * org.nrg.tip.dicom.strategy.orm.dcm4chee.Dcm4cheeResultSetLimitStrategy
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.strategy.orm.dcm4chee;

import org.nrg.tip.dicom.strategy.orm.BasicResultSetLimitStrategy;
import org.nrg.tip.dto.PacsSearchCriteria;

public class Dcm4cheeResultSetLimitStrategy extends BasicResultSetLimitStrategy {

    @Override
    public boolean searchCriteriaIsSufficientlySpecific(PacsSearchCriteria searchCriteria) {
        // with our own PACS we don't care about overloading it, allow "SELECT * FROM PACS"-style searches
        return true;
    }
}
