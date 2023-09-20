/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.QueuedPacsRequestService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;

import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/19/18.
 */
public interface QueuedPacsRequestService extends BasePacsRequestService<QueuedPacsRequest> {
    String TYPE = "queued";

    List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId);

    List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId, final PaginatedPacsRequest request);

    boolean isQueued(String requestId, final String studyInstanceUid);
}
