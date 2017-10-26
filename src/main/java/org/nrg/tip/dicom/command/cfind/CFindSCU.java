/*
 * org.nrg.tip.dicom.command.cfind.CFindSCU
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.command.cfind;

import org.nrg.tip.domain.Patient;
import org.nrg.tip.domain.Series;
import org.nrg.tip.domain.Study;
import org.nrg.tip.dto.PacsSearchCriteria;
import org.nrg.tip.dto.PacsSearchResults;

public interface CFindSCU {

    PacsSearchResults<String, Patient> cfindPatientsByExample(final PacsSearchCriteria searchCriteria);

    Patient cfindPatientById(final String patientId);

    PacsSearchResults<String, Study> cfindStudiesByExample(final PacsSearchCriteria searchCriteria);

    Study cfindStudyById(final String studyInstanceUid);

    PacsSearchResults<String, Series> cfindSeriesByStudy(final Study Study);

    Series cfindSeriesById(final String seriesInstanceUid);
}
