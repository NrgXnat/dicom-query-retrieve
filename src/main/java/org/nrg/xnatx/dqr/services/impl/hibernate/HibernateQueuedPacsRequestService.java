/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernateQueuedPacsRequestService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import lombok.extern.slf4j.Slf4j;
import org.nrg.xnatx.dqr.domain.daos.QueuedPacsRequestDAO;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/19/18.
 */
@Slf4j
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

    @Override
    public boolean isQueued(final String studyInstanceUid) {
        return getDao().isQueuedForStudyInstanceUid(studyInstanceUid);
    }

    @Override
    public void deleteAllWithRequestIdAndStatus(final String requestId, final List<String> statuses) {
        final Map<String, Object> properties = new HashMap<>();
        properties.put("requestId", requestId);
        properties.put("status", statuses);

        getDao().findByProperties(properties).forEach(r -> {
            try {
                delete(r.getId());
            } catch (Exception e) {
                log.error("An unexpected error occurred while attempting to delete pacs request with id {}", r.getId(), e);
            }
        });
    }
}
