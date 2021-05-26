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
import lombok.NoArgsConstructor;

import java.util.Collection;
import java.util.Optional;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class PacsSearchResults<V> {
    @SuppressWarnings("unchecked")
    public static <V> PacsSearchResults<V> emptyResults() {
        return (PacsSearchResults<V>) EMPTY_RESULTS;
    }

    @JsonIgnore
    public Optional<V> getFirstResult() {
        return results == null || results.isEmpty() ? Optional.empty() : Optional.ofNullable(results.iterator().next());
    }

    private static final PacsSearchResults<?> EMPTY_RESULTS = PacsSearchResults.builder().build();

    private Collection<V>              results;
    private boolean                    hasLimitedResultSetSize;
    private StudyDateRangeLimitResults studyDateRangeLimitResults;
}
