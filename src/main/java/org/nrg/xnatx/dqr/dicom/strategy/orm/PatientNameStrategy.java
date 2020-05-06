/*
 * PatientNameStrategy
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

import org.nrg.xnatx.dqr.domain.PatientName;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;

public interface PatientNameStrategy {

    PatientName dicomPatientNameToDqrPatientName(String dicomPatientName);

    DicomPersonNameSearchCriteria dqrSearchCriteriaToDicomSearchCriteria(PacsSearchCriteria searchCriteria);
}
