/*
 * PacsStudyImportRequest
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
import java.util.Date;
import java.util.List;
import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.Accessors;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;

@Data
@Accessors(prefix = "_")
@Builder
public class PacsStudyImportRequest implements Serializable {
    private final Pacs                        _pacs;
    private final Study                       _study;
    private final Date                        _dateRequested;
    private final String                      _requestingUser;
    @Singular
    private final List<PacsScanImportRequest> _scans;
}
