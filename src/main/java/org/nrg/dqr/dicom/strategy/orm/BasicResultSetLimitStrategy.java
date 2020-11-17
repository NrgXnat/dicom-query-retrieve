/*
 * BasicResultSetLimitStrategy
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

import java.util.HashMap;
import java.util.Map;

import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.domain.PatientName;
import org.nrg.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnat.utils.DateRange;
import org.nrg.xnat.utils.DqrDateRange;

public class BasicResultSetLimitStrategy implements ResultSetLimitStrategy {

    private final Map<QueryRetrieveLevel, Integer> maxResults;

    private StudyDateRangeLimitStrategy studyDateRangeLimitStrategy;

    public BasicResultSetLimitStrategy() {
        this(50, 500, new NoLimitStudyDateRangeLimitStrategy());
    }

    public BasicResultSetLimitStrategy(final int maxPatientLevelResults, final int maxStudyLevelResults,
                                       final StudyDateRangeLimitStrategy studyDateRangeLimitStrategy) {
        maxResults = new HashMap<QueryRetrieveLevel, Integer>(5);
        maxResults.put(QueryRetrieveLevel.PATIENT, maxPatientLevelResults);
        maxResults.put(QueryRetrieveLevel.STUDY, maxStudyLevelResults);
        maxResults.put(QueryRetrieveLevel.SERIES, Integer.MAX_VALUE);
        setStudyDateRangeLimitStrategy(studyDateRangeLimitStrategy);
    }

    @Override
    public int getMaxResultsForQueryLevel(QueryRetrieveLevel level) {
        return maxResults.get(level);
    }

    @Override
    public boolean searchCriteriaIsSufficientlySpecific(final PacsSearchCriteria searchCriteria) {
        if (searchCriteria.isAtLeastOneKeyCriterionSpecified()) {
            return true;
        } else {
            return patientNameIsSufficientlySpecificAsTheSoleSearchCriterion(new PatientName(
                    searchCriteria.getPatientName()))
                    || studyDateRangeIsSufficientlySpecificAsTheSoleSearchCriterion(searchCriteria.getStudyDateRange());
        }
    }

    private boolean patientNameIsSufficientlySpecificAsTheSoleSearchCriterion(final PatientName patientName) {
        if (null == patientName || patientName.isBlank() || !patientName.hasLastName()) {
            return false;
        } else {
            String lastNameWithWildcardsStripped = patientName.getLastName().replaceAll("\\*", "");
            return StringUtils.trimToEmpty(lastNameWithWildcardsStripped).length() >= minimumLengthOfPatientNameAsTheSoleSearchCriterion();
        }
    }

    protected int minimumLengthOfPatientNameAsTheSoleSearchCriterion() {
        return 1;
    }

    private boolean studyDateRangeIsSufficientlySpecificAsTheSoleSearchCriterion(final DqrDateRange dateRange) {
        return null != dateRange && dateRange.isBounded();
    }

    @Override
    public StudyDateRangeLimitResults limitStudyDateRange(PacsSearchCriteria searchCriteria) {
        return getStudyDateRangeLimitStrategy().limitStudyDateRange(searchCriteria);
    }

    public StudyDateRangeLimitStrategy getStudyDateRangeLimitStrategy() {
        return studyDateRangeLimitStrategy;
    }

    public void setStudyDateRangeLimitStrategy(final StudyDateRangeLimitStrategy studyDateRangeLimitStrategy) {
        this.studyDateRangeLimitStrategy = studyDateRangeLimitStrategy;
    }
}
