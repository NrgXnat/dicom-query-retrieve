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

package org.nrg.dqr.messaging;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.StringUtils;

import java.io.Serializable;

@Getter
@Builder
@AllArgsConstructor
public class PacsScanExportRequest implements Serializable {
    @Override
    public String toString() {
        return StringUtils.defaultIfBlank(scanId, "null");
    }

    private final String scanId;
}
