/*
 * ReferringPhysicianName
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.domain;

import org.dcm4che2.data.PersonName;

public class ReferringPhysicianName extends DqrPersonName {

    private static final long serialVersionUID = 1L;

    public ReferringPhysicianName(){

    }

    public ReferringPhysicianName(PersonName personName) {
        super(personName);
    }

    public ReferringPhysicianName(String firstName, String lastName) {
        super(firstName, lastName);
    }
}
