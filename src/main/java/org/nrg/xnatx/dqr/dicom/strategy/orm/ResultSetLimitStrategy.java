/*
 * ResultSetLimitStrategy
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.strategy.orm;

import org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;

public interface ResultSetLimitStrategy {
    boolean searchCriteriaIsSufficientlySpecific(final PacsSearchCriteria searchCriteria);

    int getMaxResultsForQueryLevel(final QueryRetrieveLevel level);

    StudyDateRangeLimitResults limitStudyDateRange(final PacsSearchCriteria searchCriteria);
}
