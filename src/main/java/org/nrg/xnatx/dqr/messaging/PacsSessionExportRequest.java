/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsSessionExportRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.Builder;
import lombok.Data;
import lombok.Singular;
import lombok.experimental.Accessors;
import org.nrg.xnatx.dqr.domain.entities.Pacs;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Data
@Accessors(prefix = "_")
@Builder
public class PacsSessionExportRequest implements Serializable {
    private static final long serialVersionUID = 6524490305495256495L;

    private final Pacs                        _pacs;
    private final String                      _sessionId;
    private final Date                        _dateRequested;
    private final String                      _requestingUser;
    @Singular
    private final List<PacsScanExportRequest> _scans;
}
