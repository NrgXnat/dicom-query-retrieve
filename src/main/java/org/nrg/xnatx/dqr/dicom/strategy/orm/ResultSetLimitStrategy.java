/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.strategy.orm.ResultSetLimitStrategy
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm;

import org.nrg.xnatx.dqr.dicom.command.dcm4che3.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;

public interface ResultSetLimitStrategy {
    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    boolean searchCriteriaIsSufficientlySpecific(final PacsSearchCriteria searchCriteria);

    int getMaxResultsForQueryLevel(final QueryRetrieveLevel level);

    StudyDateRangeLimitResults limitStudyDateRange(final PacsSearchCriteria searchCriteria);
}
