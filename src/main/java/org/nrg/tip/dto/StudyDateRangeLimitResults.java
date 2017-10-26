/*
 * org.nrg.tip.dto.StudyDateRangeLimitResults
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dto;

import org.nrg.xnat.utils.DateRange;


public class StudyDateRangeLimitResults {

    public enum LimitType {
        NO_LIMIT, RECENT_STUDIES_LIMIT
    }

    private final LimitType limitType;

    private final DateRange dateRange;

    private final String limitExplanation;

    public StudyDateRangeLimitResults(final LimitType limitType, final DateRange dateRange,
                                      final String limitExplanation) {
        this.limitType = limitType;
        this.dateRange = dateRange;
        this.limitExplanation = limitExplanation;
    }

    public StudyDateRangeLimitResults(final LimitType limitType, final DateRange dateRange) {
        this(limitType, dateRange, null);
    }

    public boolean isUnlimited() {
        return null == limitType || limitType.equals(LimitType.NO_LIMIT);
    }

    public boolean isLimited() {
        return !isUnlimited();
    }

    public DateRange getDateRange() {
        return dateRange;
    }

    public String getLimitExplanation() {
        return limitExplanation;
    }
}
