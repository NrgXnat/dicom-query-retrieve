/*
 * RecentStudiesStudyDateRangeLimitStrategy
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dicom.strategy.orm;

import java.util.Calendar;
import java.util.Date;

import org.apache.commons.lang.time.DateUtils;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnat.utils.DateRange;

public class RecentStudiesStudyDateRangeLimitStrategy implements StudyDateRangeLimitStrategy {

    private final int numberOfDays;

    public RecentStudiesStudyDateRangeLimitStrategy(int numberOfDays) {
        this.numberOfDays = numberOfDays;
    }

    @Override
    public StudyDateRangeLimitResults limitStudyDateRange(final PacsSearchCriteria searchCriteria) {
        if (searchCriteria.isAtLeastOneKeyCriterionSpecified()) {
            return new NoLimitStudyDateRangeLimitStrategy().limitStudyDateRange(searchCriteria);
        }

        DateRange workingStudyDateRange = searchCriteria.getStudyDateRange();
        if (null == workingStudyDateRange) {
            workingStudyDateRange = new DateRange();
        }

        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DATE, -numberOfDays);
        Date earliestAllowedBeginningOfStudyDateRange = DateUtils.truncate(cal.getTime(), Calendar.DATE);

        if (workingStudyDateRange.getStart().before(earliestAllowedBeginningOfStudyDateRange)) {
            return new StudyDateRangeLimitResults(
                    StudyDateRangeLimitResults.LimitType.RECENT_STUDIES_LIMIT,
                    new DateRange(earliestAllowedBeginningOfStudyDateRange, workingStudyDateRange.getEnd()),
                    String.format(
                            "The query results have been limited to studies performed within the past %d days to avoid overtaxing the PACS.",
                            numberOfDays));
        } else {
            // the caller's query is more restrictive than what the PACS requires, so tell him we didn't limit him
            return new NoLimitStudyDateRangeLimitStrategy().limitStudyDateRange(searchCriteria);
        }
    }

    public int getNumberOfDays() {
        return numberOfDays;
    }
}
