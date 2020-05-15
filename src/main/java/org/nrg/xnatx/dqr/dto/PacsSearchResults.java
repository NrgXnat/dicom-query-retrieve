/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.PacsSearchResults
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;

@Data
@Accessors(prefix = "_")
@Builder
@AllArgsConstructor
public class PacsSearchResults<K, V> {
    @SuppressWarnings("unchecked")
    public static <K, V> PacsSearchResults<K, V> emptyResults() {
        return (PacsSearchResults<K, V>) EMPTY_RESULTS;
    }

    public V getFirstResult() {
        return _results == null || _results.isEmpty() ? null : _results.values().iterator().next();
    }

    @SuppressWarnings("rawtypes")
    private static final PacsSearchResults EMPTY_RESULTS = PacsSearchResults.builder().build();

    private final Map<K, V>                  _results;
    private final boolean                    _hasLimitedResultSetSize;
    private final StudyDateRangeLimitResults _studyDateRangeLimitResults;
}
