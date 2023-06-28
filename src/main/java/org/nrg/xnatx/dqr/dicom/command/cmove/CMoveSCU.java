/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cmove.CMoveSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cmove;

import org.nrg.xnatx.dqr.domain.DimseRequestContext;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;

public interface CMoveSCU {

    /**
     * Sends a C-MOVE request to retrieve a series.
     *
     * @param context Request context in which this operation occurs.
     * @param study  The study to which the desired series belongs.
     * @param series The desired series.
     */
    void cmoveSeries(DimseRequestContext context, Study study, Series series);
}
