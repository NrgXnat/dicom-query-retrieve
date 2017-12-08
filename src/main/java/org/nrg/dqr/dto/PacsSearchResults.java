/*
 * PacsSearchResults
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dto;

import java.util.Collection;
import java.util.Map;


public class PacsSearchResults<K, V> {

    protected final Map<K, V> results;

    private final boolean hasLimitedResultSetSize;

    private final StudyDateRangeLimitResults studyDateRangeLimitResults;

    public PacsSearchResults(final Map<K, V> results, final boolean hasLimitedResultSetSize,
                             final StudyDateRangeLimitResults studyDateRangeLimitResults) {
        this.results = results;
        this.hasLimitedResultSetSize = hasLimitedResultSetSize;
        this.studyDateRangeLimitResults = studyDateRangeLimitResults;
    }

    public Collection<V> getResults() {
        return results.values();
    }

    public V getResult(final K key) {
        return results.get(key);
    }

    public V getFirstResult() {
        return 0 == results.size() ? null : getResults().iterator().next();
    }

    public int getResultSize() {
        return results.size();
    }

    /**
     * Did we hit our max results and therefore artifically limit the result set?
     *
     * @return
     */
    public boolean hasLimitedResultSetSize() {
        return hasLimitedResultSetSize;
    }

    /**
     * Did we artifically limit the study date range in the query?
     *
     * @return
     */
    public boolean hasLimitedStudyDateResults() {
        return null != studyDateRangeLimitResults && studyDateRangeLimitResults.isLimited();
    }

    public StudyDateRangeLimitResults getStudyDateRangeLimitResults() {
        return studyDateRangeLimitResults;
    }
}
