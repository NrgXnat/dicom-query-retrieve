/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.strategy.orm.NoLimitStudyDateRangeLimitStrategy
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm;

import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;

public class NoLimitStudyDateRangeLimitStrategy implements StudyDateRangeLimitStrategy {

    @Override
    public StudyDateRangeLimitResults limitStudyDateRange(final PacsSearchCriteria searchCriteria) {
        return new StudyDateRangeLimitResults(StudyDateRangeLimitResults.LimitType.NO_LIMIT,
                                              searchCriteria.getStudyDateRange());
    }
}
