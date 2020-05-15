/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsScanImportRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;

@Data
@Accessors(prefix = "_")
@Builder
public class PacsScanImportRequest implements Serializable {
    @Override
    public String toString() {
        return getStudy().getStudyInstanceUid() + ":" + getSeries().getSeriesInstanceUid();
    }

    private final Series _series;
    private final Study  _study;
}
