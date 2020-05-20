/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernateQueuedPacsRequestService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import java.util.List;
import java.util.Map;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.daos.QueuedPacsRequestDAO;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by mike on 1/19/18.
 */
@Service
@Transactional
public class HibernateQueuedPacsRequestService extends AbstractHibernatePacsRequestService<QueuedPacsRequest, QueuedPacsRequestDAO> implements QueuedPacsRequestService {
    @Autowired
    public HibernateQueuedPacsRequestService(final NamedParameterJdbcTemplate template) {
        _template = template;
    }

    @Override
    public List<Map<String, Object>> getAllWithOrder() {
        return _template.queryForList(QUERY_QUEUE_WITH_LOCATION, EmptySqlParameterSource.INSTANCE);
    }

    @Override
    public List<Map<String, Object>> getAllWithOrderForUser(final UserI user) {
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("user", user.getUsername());
        return _template.queryForList(QUERY_QUEUE_WITH_LOCATION_FOR_USER, parameters);
    }

    @Override
    public List<QueuedPacsRequest> getAllForPacsOrderedByPriorityAndDate(final long pacsId) {
        return getDao().findAllForPacsOrderedByPriorityAndDate(pacsId);
    }

    @Override
    public List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId) {
        return getDao().findQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId);
    }

    private static final String QUERY_QUEUE_WITH_LOCATION          = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue;";
    private static final String QUERY_QUEUE_WITH_LOCATION_FOR_USER = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue WHERE username=:user;";

    private final NamedParameterJdbcTemplate _template;
}
