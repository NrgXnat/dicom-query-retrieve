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
    public List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId) {
        return getDao().findQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId);
    }

    @Override
    public List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId, final PaginatedPacsRequest request) {
        return getDao().findQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId, request);
    }

    private static final String QUERY_QUEUE_WITH_LOCATION          = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue;";
    private static final String QUERY_QUEUE_WITH_LOCATION_FOR_USER = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue WHERE username=:user;";
}
