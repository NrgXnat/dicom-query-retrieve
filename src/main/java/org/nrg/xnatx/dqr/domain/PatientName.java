/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.PatientName
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain;

import org.dcm4che2.data.PersonName;

public class PatientName extends DqrPersonName {

    private static final long serialVersionUID = 1L;

    public PatientName() {
        super();
    }

    public PatientName(PersonName personName) {
        super(personName);
    }

    public PatientName(String firstName, String lastName, String middleName, String prefix, String suffix) {
        super(firstName, lastName, middleName, prefix, suffix);
    }

    public PatientName(String firstName, String lastName) {
        super(firstName, lastName);
    }

    public PatientName(String commaDelimitedName) {
        super(commaDelimitedName);
    }
}
