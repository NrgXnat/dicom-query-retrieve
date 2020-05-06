/*
 * CMoveSCU
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.command.cmove;

import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;

public interface CMoveSCU {

    /**
     * Sends a C-MOVE request to retrieve a series.
     *
     * @param study     The study to which the desired series belongs.
     * @param series    The desired series.
     */
    void cmoveSeries(final Study study, final Series series);
}
