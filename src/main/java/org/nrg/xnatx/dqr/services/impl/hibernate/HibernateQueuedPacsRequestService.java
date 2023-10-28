/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernateQueuedPacsRequestService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.nrg.xnatx.dqr.domain.daos.QueuedPacsRequestDAO;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
@Transactional
public class HibernateQueuedPacsRequestService extends AbstractHibernatePacsRequestService<QueuedPacsRequest, QueuedPacsRequestDAO> implements QueuedPacsRequestService {
    @Autowired
    public HibernateQueuedPacsRequestService(final NamedParameterJdbcTemplate template) {
        super(template);
    }

    @Override
    protected String getRequestType() {
        return TYPE;
    }

    @Override
    public List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId) {
        return getDao().findQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId);
    }

    @Override
    public List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId, final PaginatedPacsRequest request) {
        return getDao().findQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId, request);
    }

    @Override
    public boolean isQueued(String requestId, final String studyInstanceUid) {
        return getDao().isQueuedForStudyInstanceUidAndRequestId(requestId, studyInstanceUid);
    }
}
