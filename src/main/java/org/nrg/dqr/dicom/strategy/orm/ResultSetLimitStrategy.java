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

package org.nrg.dqr.dicom.strategy.orm;

import org.nrg.dqr.dicom.command.cfind.dcm4che.tool.DcmQRXnat.QueryRetrieveLevel;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.StudyDateRangeLimitResults;

public interface ResultSetLimitStrategy {

    boolean searchCriteriaIsSufficientlySpecific(PacsSearchCriteria searchCriteria);

    int getMaxResultsForQueryLevel(QueryRetrieveLevel level);

    StudyDateRangeLimitResults limitStudyDateRange(PacsSearchCriteria searchCriteria);
}
