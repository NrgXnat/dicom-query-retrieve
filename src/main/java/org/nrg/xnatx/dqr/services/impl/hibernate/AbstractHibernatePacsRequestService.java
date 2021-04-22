/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.AbstractHibernatePacsRequestService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.daos.AbstractPacsRequestDAO;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.services.BasePacsRequestService;
import org.springframework.jdbc.core.namedparam.EmptySqlParameterSource;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Transactional
public abstract class AbstractHibernatePacsRequestService<R extends PacsRequest, DAO extends AbstractPacsRequestDAO<R>> extends AbstractHibernateEntityService<R, DAO> implements BasePacsRequestService<R> {
    private static final String QUERY_QUEUE_WITH_LOCATION          = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue;";
    private static final String QUERY_QUEUE_WITH_LOCATION_FOR_USER = "SELECT * FROM (SELECT row_number() over(partition by pacs_id ORDER BY priority, queued_time) AS queue_location, * FROM xhbm_queued_pacs_request ORDER BY priority, queued_time) AS queue WHERE username=:user;";

    private final NamedParameterJdbcTemplate _template;

    protected AbstractHibernatePacsRequestService(final NamedParameterJdbcTemplate template) {
        _template = template;
    }

    @Override
    public long getAllForUserCount(final UserI user) {
        return getAllForUser(user).size();
    }

    @Override
    public List<R> getAllForUser(final UserI user) {
        return getDao().findAllByUser(user);
    }

    // TODO: There's no findById() that considers the user security.
    public List<R> getAllForUser(final UserI user, final PaginatedPacsRequest request) {
        return getDao().findAllByUser(user, request);
    }

    @Override
    public R getByIdForUser(final long id, final UserI user) {
        return getDao().findById(id);
    }

    public List<R> getAllOrderedByDate() {
        return getDao().findAllOrderedByDate();
    }

    public List<R> getAllOrderedByDate(final PaginatedPacsRequest request) {
        return getDao().findAllOrderedByDate(request);
    }

    @Override
    public R getMostRecentForPacs(final long pacsId) {
        return instance(getDao().findByPacsIdOrderedByMostRecent(pacsId));
    }

    @Override
    public R getMostRecentForStudyInstanceUid(final String studyInstanceUid) {
        return instance(getDao().findByStudyInstanceUidOrderedByMostRecent(studyInstanceUid));
    }
    public List<Map<String, Object>> getAllWithOrder() {
        return _template.queryForList(QUERY_QUEUE_WITH_LOCATION, EmptySqlParameterSource.INSTANCE);
    }

    public List<Map<String, Object>> getAllWithOrderForUser(final UserI user) {
        return _template.queryForList(QUERY_QUEUE_WITH_LOCATION_FOR_USER, new MapSqlParameterSource("user", user.getUsername()));
    }

    public List<R> getAllForPacsOrderedByPriorityAndDate(final long pacsId) {
        return getDao().findAllForPacsOrderedByPriorityAndDate(pacsId);
    }

    public List<R> getAllForPacsOrderedByPriorityAndDate(final long pacsId, final PaginatedPacsRequest request) {
        return getDao().findAllForPacsOrderedByPriorityAndDate(pacsId);
    }
}
