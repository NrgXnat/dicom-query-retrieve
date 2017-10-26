/*
 * org.nrg.tip.dicom.strategy.orm.NoLimitStudyDateRangeLimitStrategy
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.strategy.orm;

import org.nrg.tip.dto.PacsSearchCriteria;
import org.nrg.tip.dto.StudyDateRangeLimitResults;

public class NoLimitStudyDateRangeLimitStrategy implements StudyDateRangeLimitStrategy {

    @Override
    public StudyDateRangeLimitResults limitStudyDateRange(final PacsSearchCriteria searchCriteria) {
        return new StudyDateRangeLimitResults(StudyDateRangeLimitResults.LimitType.NO_LIMIT,
                searchCriteria.getStudyDateRange());
    }
}
