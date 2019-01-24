package org.nrg.dqr.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.dqr.daos.ExecutedPacsRequestDAO;
import org.nrg.dqr.daos.QueuedPacsRequestDAO;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.net.URLEncoder;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernateQueuedPacsRequestService extends AbstractHibernateEntityService<QueuedPacsRequest, QueuedPacsRequestDAO> implements QueuedPacsRequestService {

    private static final Log _log = LogFactory.getLog(HibernateQueuedPacsRequestService.class);

    @Inject
    private QueuedPacsRequestDAO _dao;

    @Inject
    private NamedParameterJdbcTemplate _parameterized;

    @Override
    @Transactional
    public List<QueuedPacsRequest> getAllOrderedByDate(){
        return getDao().findAllOrderedByDate();
    }

    @Override
    @Transactional
    public List<QueuedPacsRequest> getAllForUser(UserI user){
        return _dao.findAllForUser(user);
    }

    @Override
    @Transactional
    public List<Map<String, Object>> getAllWithOrder(){
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        Map<Long, QueuedPacsRequest> queueMap = new HashMap<>();
        final List<Map<String, Object>> results = _parameterized.queryForList(QUERY_QUEUE_WITH_LOCATION, parameters);
        return results;
    }

    @Override
    @Transactional
    public List<Map<String, Object>> getAllWithOrderForUser(UserI user){
        final MapSqlParameterSource parameters = new MapSqlParameterSource();
        parameters.addValue("user", user.getUsername());
        Map<Long, QueuedPacsRequest> queueMap = new HashMap<>();
        final List<Map<String, Object>> results = _parameterized.queryForList(QUERY_QUEUE_WITH_LOCATION_FOR_USER, parameters);
        return results;
    }

    @Override
    @Transactional
    public QueuedPacsRequest getByIdForUser(Long id, UserI user){
        List<QueuedPacsRequest> list = _dao.findByIdForUser(id, user);
        if(list==null || list.size()==0){
            return null;
        }
        else{
            return list.get(0);
        }
    }

    @Override
    @Transactional
    public List<QueuedPacsRequest> getAllForPacsOrderedByPriorityAndDate(Long pacsId){
        return _dao.findAllForPacsOrderedByPriorityAndDate(pacsId);
    }

    private static final String QUERY_QUEUE_WITH_LOCATION = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue;";
    private static final String QUERY_QUEUE_WITH_LOCATION_FOR_USER = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue WHERE username=:user;";
}
