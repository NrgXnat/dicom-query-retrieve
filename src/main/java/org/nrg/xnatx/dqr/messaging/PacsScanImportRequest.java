/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsScanImportRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;

import java.io.Serializable;

@Data
@Accessors(prefix = "_")
@AllArgsConstructor
@Builder
public class PacsScanImportRequest implements Serializable {
    private static final long serialVersionUID = -199459308378280991L;

    @Override
    public String toString() {
        return getStudy().getStudyInstanceUid() + ":" + getSeries().getSeriesInstanceUid();
    }

    private final Series _series;
    private final Study  _study;
}
