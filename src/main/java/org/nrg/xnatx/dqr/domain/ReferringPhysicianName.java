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
    private static final long serialVersionUID = 4641995793372862768L;

    public ReferringPhysicianName(final PersonName personName) {
        super(personName);
    }

    public ReferringPhysicianName(final String firstName, final String lastName) {
        super(firstName, lastName);
    }
}
