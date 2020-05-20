/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.strategy.orm.BasicResultSetLimitStrategy
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm;

import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.domain.DqrPersonName;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import javax.annotation.Nonnull;
import java.util.HashMap;
import java.util.Map;

public class BasicResultSetLimitStrategy implements ResultSetLimitStrategy {
    public BasicResultSetLimitStrategy() {
        this(50, 500, new NoLimitStudyDateRangeLimitStrategy());
    }

    public BasicResultSetLimitStrategy(final int maxPatientLevelResults, final int maxStudyLevelResults, final StudyDateRangeLimitStrategy studyDateRangeLimitStrategy) {
        _maxResults = new HashMap<>(5);
        _maxResults.put(QueryRetrieveLevel.PATIENT, maxPatientLevelResults);
        _maxResults.put(QueryRetrieveLevel.STUDY, maxStudyLevelResults);
        _maxResults.put(QueryRetrieveLevel.SERIES, Integer.MAX_VALUE);
        _studyDateRangeLimitStrategy = studyDateRangeLimitStrategy;
    }

    @Override
    public int getMaxResultsForQueryLevel(final QueryRetrieveLevel level) {
        return _maxResults.get(level);
    }

    @Override
    public boolean searchCriteriaIsSufficientlySpecific(final PacsSearchCriteria searchCriteria) {
        return searchCriteria.isAtLeastOneKeyCriterionSpecified() ||
               patientNameIsSufficientlySpecificAsTheSoleSearchCriterion(new DqrPersonName(searchCriteria.getPatientName())) ||
               studyDateRangeIsSufficientlySpecificAsTheSoleSearchCriterion(searchCriteria.getStudyDateRange());
    }

    @Override
    public StudyDateRangeLimitResults limitStudyDateRange(PacsSearchCriteria searchCriteria) {
        return getStudyDateRangeLimitStrategy().limitStudyDateRange(searchCriteria);
    }

    public StudyDateRangeLimitStrategy getStudyDateRangeLimitStrategy() {
        return _studyDateRangeLimitStrategy;
    }

    protected int minimumLengthOfPatientNameAsTheSoleSearchCriterion() {
        return 1;
    }

    private boolean patientNameIsSufficientlySpecificAsTheSoleSearchCriterion(final DqrPersonName patientName) {
        return patientName != null &&
               !patientName.isBlank() &&
               patientName.hasLastName() &&
               StringUtils.trimToEmpty(patientName.getLastName().replaceAll("\\*", "")).length() >= minimumLengthOfPatientNameAsTheSoleSearchCriterion();
    }

    private boolean studyDateRangeIsSufficientlySpecificAsTheSoleSearchCriterion(final @Nonnull DqrDateRange dateRange) {
        return dateRange.isBounded();
    }

    private final Map<QueryRetrieveLevel, Integer> _maxResults;
    private final StudyDateRangeLimitStrategy      _studyDateRangeLimitStrategy;
}
