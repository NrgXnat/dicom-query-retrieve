/*
 * CFindSCU
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.command.cfind;

import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;

public interface CFindSCU {

    PacsSearchResults<String, Patient> cfindPatientsByExample(final PacsSearchCriteria searchCriteria);

    Patient cfindPatientById(final String patientId);

    PacsSearchResults<String, Study> cfindStudiesByExample(final PacsSearchCriteria searchCriteria);

    Study cfindStudyById(final String studyInstanceUid);

    PacsSearchResults<String, Series> cfindSeriesByStudy(final Study Study);

    PacsSearchResults<String, Series> cfindSeriesByStudyUid(final String studyUid);

    Series cfindSeriesById(final String seriesInstanceUid);
}
