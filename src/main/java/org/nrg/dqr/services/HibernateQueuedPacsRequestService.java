package org.nrg.dqr.services;

import org.nrg.dqr.daos.QueuedPacsRequestDAO;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernateQueuedPacsRequestService extends AbstractHibernateEntityService<QueuedPacsRequest, QueuedPacsRequestDAO> implements QueuedPacsRequestService {
    public HibernateQueuedPacsRequestService(final NamedParameterJdbcTemplate template) {
        _template = template;
    }

    @Override
    @Transactional
    public List<QueuedPacsRequest> getAllOrderedByDate() {
        return getDao().findAllOrderedByDate();
    }

    @Override
    @Transactional
    public List<QueuedPacsRequest> getAllForUser(final UserI user) {
        return getDao().findAllForUser(user);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> getAllWithOrder() {
        return _template.queryForList(QUERY_QUEUE_WITH_LOCATION, EmptySqlParameterSource.INSTANCE);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> getAllWithOrderForUser(UserI user) {
        return _template.queryForList(QUERY_QUEUE_WITH_LOCATION_FOR_USER, new MapSqlParameterSource("user", user.getUsername()));
    }

    @Override
    @Transactional
    public QueuedPacsRequest getByIdForUser(Long id, UserI user) {
        final List<QueuedPacsRequest> list = getDao().findByIdForUser(id, user);
        return list == null || list.isEmpty() ? null : list.get(0);
    }

    @Override
    @Transactional
    public List<QueuedPacsRequest> getAllForPacsOrderedByPriorityAndDate(Long pacsId) {
        return getDao().findAllForPacsOrderedByPriorityAndDate(pacsId);
    }

    @Override
    @Transactional
    public List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(Long pacsId) {
        return getDao().findQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId);
    }

    private static final String QUERY_QUEUE_WITH_LOCATION          = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue;";
    private static final String QUERY_QUEUE_WITH_LOCATION_FOR_USER = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue WHERE username=:user;";

    private final NamedParameterJdbcTemplate _template;
}
