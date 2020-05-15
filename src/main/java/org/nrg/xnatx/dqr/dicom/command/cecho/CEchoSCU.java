/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cecho;

public interface CEchoSCU {

    void cecho();

    boolean canConnect();
}
