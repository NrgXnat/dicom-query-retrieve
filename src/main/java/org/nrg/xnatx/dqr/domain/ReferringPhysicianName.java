/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.ReferringPhysicianName
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain;

import org.dcm4che2.data.PersonName;

public class ReferringPhysicianName extends DqrPersonName {

    private static final long serialVersionUID = 1L;

    public ReferringPhysicianName() {

    }

    public ReferringPhysicianName(PersonName personName) {
        super(personName);
    }

    public ReferringPhysicianName(String firstName, String lastName) {
        super(firstName, lastName);
    }
}
