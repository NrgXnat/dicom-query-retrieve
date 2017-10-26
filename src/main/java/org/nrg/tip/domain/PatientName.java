/*
 * org.nrg.tip.domain.PatientName
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.domain;

import org.dcm4che2.data.PersonName;

public class PatientName extends TipPersonName {

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
