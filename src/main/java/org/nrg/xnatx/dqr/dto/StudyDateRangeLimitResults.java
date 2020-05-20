/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Value;
import lombok.experimental.Accessors;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

@Value
@Accessors(prefix = "_")
@AllArgsConstructor
@Builder
public class StudyDateRangeLimitResults {
    public enum LimitType {
        NO_LIMIT, RECENT_STUDIES_LIMIT
    }

    public StudyDateRangeLimitResults(final LimitType limitType, final DqrDateRange dateRange) {
        this(limitType, dateRange, null);
    }

    public boolean isUnlimited() {
        return null == _limitType || _limitType.equals(LimitType.NO_LIMIT);
    }

    public boolean isLimited() {
        return !isUnlimited();
    }

    @Builder.Default
    LimitType    _limitType        = null;
    @Builder.Default
    DqrDateRange _dateRange        = null;
    @Builder.Default
    String       _limitExplanation = null;
}
