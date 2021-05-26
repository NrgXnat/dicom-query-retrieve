/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.Serializable;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class StudyDateRangeLimitResults implements Serializable {
    private static final long serialVersionUID = 7707527574121492161L;

    public enum LimitType {
        NO_LIMIT, RECENT_STUDIES_LIMIT
    }

    public StudyDateRangeLimitResults(final LimitType limitType, final DqrDateRange dateRange) {
        this(limitType, dateRange, null);
    }

    @JsonIgnore
    public boolean isUnlimited() {
        return limitType.equals(LimitType.NO_LIMIT);
    }

    @JsonIgnore
    public boolean isLimited() {
        return !isUnlimited();
    }

    @Builder.Default
    private LimitType    limitType        = LimitType.NO_LIMIT;
    @Builder.Default
    private DqrDateRange dateRange        = null;
    @Builder.Default
    private String       limitExplanation = null;
}
