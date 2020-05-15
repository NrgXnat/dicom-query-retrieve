/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.strategy.orm.RecentStudiesStudyDateRangeLimitStrategy
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm;

import java.util.Calendar;
import java.util.Date;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.time.DateUtils;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

@Data
@Accessors(prefix = "_")
public class RecentStudiesStudyDateRangeLimitStrategy implements StudyDateRangeLimitStrategy {
    @Override
    public StudyDateRangeLimitResults limitStudyDateRange(final PacsSearchCriteria searchCriteria) {
        if (searchCriteria.isAtLeastOneKeyCriterionSpecified()) {
            return new NoLimitStudyDateRangeLimitStrategy().limitStudyDateRange(searchCriteria);
        }

        final DqrDateRange workingStudyDateRange = ObjectUtils.defaultIfNull(searchCriteria.getStudyDateRange(), new DqrDateRange());
        final Calendar     calendar              = Calendar.getInstance();
        calendar.add(Calendar.DATE, -_numberOfDays);
        final Date earliestAllowedBeginningOfStudyDateRange = DateUtils.truncate(calendar.getTime(), Calendar.DATE);

        if (workingStudyDateRange.getStart().before(earliestAllowedBeginningOfStudyDateRange)) {
            return new StudyDateRangeLimitResults(StudyDateRangeLimitResults.LimitType.RECENT_STUDIES_LIMIT,
                                                  new DqrDateRange(earliestAllowedBeginningOfStudyDateRange, workingStudyDateRange.getEnd()),
                                                  String.format(RESULTS_LIMITED_MESSAGE, _numberOfDays));
        }
        // the caller's query is more restrictive than what the PACS requires, so tell him we didn't limit him
        return new NoLimitStudyDateRangeLimitStrategy().limitStudyDateRange(searchCriteria);
    }

    private static final String RESULTS_LIMITED_MESSAGE = "The query results have been limited to studies performed within the past %d days to avoid overtaxing the PACS.";

    private final int _numberOfDays;
}
