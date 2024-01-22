/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.CFindSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind;

import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;

import java.util.Optional;

public interface CFindSCU {

    PacsSearchResults<Patient> cfindPatientsByExample(final PacsSearchCriteria searchCriteria);

    PacsSearchResults<Study> cfindStudiesByExample(final PacsSearchCriteria searchCriteria);

    Optional<Study> cfindStudyById(final String studyInstanceUid);

    PacsSearchResults<Series> cfindSeriesByExample(final PacsSearchCriteria searchCriteria);
}
