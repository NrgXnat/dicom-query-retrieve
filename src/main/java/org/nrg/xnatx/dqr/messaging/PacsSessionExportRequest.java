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
import lombok.Singular;
import lombok.Value;
import org.nrg.xnatx.dqr.domain.entities.Pacs;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Value
@Builder
public class PacsSessionExportRequest implements Serializable {
    private static final long serialVersionUID = 6524490305495256495L;

    Pacs   pacs;
    String sessionId;
    Date   dateRequested;
    String requestingUser;
    @Singular
    List<PacsScanExportRequest> scans;
}
