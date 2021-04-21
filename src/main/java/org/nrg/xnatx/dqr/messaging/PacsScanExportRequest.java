/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsScanExportRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import java.io.Serializable;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nrg.xdat.om.XnatImagescandata;

@Data
@Accessors(prefix = "_")
public class PacsScanExportRequest implements Serializable {
    private static final long serialVersionUID = 7911307125500245254L;

    @Override
    public String toString() {
        return _scan == null ? "null" : _scan.getId();
    }

    private final XnatImagescandata _scan;
}
