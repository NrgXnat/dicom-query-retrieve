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
import lombok.Value;
import lombok.experimental.Accessors;

import java.util.Collection;
import java.util.Optional;

@Value
@Accessors(prefix = "_")
@Builder
@AllArgsConstructor
public class PacsSearchResults<V> {
    @SuppressWarnings("unchecked")
    public static <V> PacsSearchResults<V> emptyResults() {
        return (PacsSearchResults<V>) EMPTY_RESULTS;
    }

    @JsonIgnore
    public Optional<V> getFirstResult() {
        return _results == null || _results.isEmpty() ? Optional.empty() : Optional.ofNullable(_results.iterator().next());
    }

    private static final PacsSearchResults<?> EMPTY_RESULTS = PacsSearchResults.builder().build();

    Collection<V>              _results;
    boolean                    _hasLimitedResultSetSize;
    StudyDateRangeLimitResults _studyDateRangeLimitResults;
}
