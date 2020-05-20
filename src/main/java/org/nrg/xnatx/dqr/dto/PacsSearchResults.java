/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.PacsSearchResults
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
import lombok.experimental.Accessors;

import java.util.Collection;

@Data
@Accessors(prefix = "_")
@Builder
@AllArgsConstructor
public class PacsSearchResults<V> {
    @SuppressWarnings("unchecked")
    public static <V> PacsSearchResults<V> emptyResults() {
        return (PacsSearchResults<V>) EMPTY_RESULTS;
    }

    @JsonIgnore
    public V getFirstResult() {
        return _results == null || _results.isEmpty() ? null : _results.iterator().next();
    }

    @SuppressWarnings("rawtypes")
    private static final PacsSearchResults EMPTY_RESULTS = PacsSearchResults.builder().build();

    private final Collection<V>              _results;
    private final boolean                    _hasLimitedResultSetSize;
    private final StudyDateRangeLimitResults _studyDateRangeLimitResults;
}
