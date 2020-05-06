/*
 * PacsScanExportRequest
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.messaging;

import java.io.Serializable;
import lombok.Data;
import lombok.experimental.Accessors;
import org.nrg.xdat.om.XnatImagescandata;

@Data
@Accessors(prefix = "_")
public class PacsScanExportRequest implements Serializable {
    @Override
    public String toString() {
        return _scan == null ? "null" : _scan.getId();
    }

    private final XnatImagescandata _scan;
}
