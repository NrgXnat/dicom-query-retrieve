/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsScanExportRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.Builder;
import lombok.Value;

import java.io.Serializable;

@Value
@Builder
public class PacsScanExportRequest implements Serializable {
    private static final long serialVersionUID = 7911307125500245254L;

    @Override
    public String toString() {
        return Integer.toString(imageScanDataId);
    }

    int imageScanDataId;
}
