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
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.daos.AbstractPacsRequestDAO;
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

    protected abstract String getRequestType();

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAllForUserCount(final UserI user) {
        return getAllForUserCount(user.getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getAllForUserCount(final String username) {
        return getAllForUser(username).size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<R> getAllForUser(final UserI user) {
        return getAllForUser(user.getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<R> getAllForUser(final String username) {
        return getDao().findAllByUsername(username);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<R> getAllForUser(final UserI user, final PaginatedPacsRequest request) {
        return getAllForUser(user.getUsername(), request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<R> getAllForUser(final String username, final PaginatedPacsRequest request) {
        return getDao().findAllByUsername(username, request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public R getByIdForUser(final long id, final UserI user) throws NotFoundException {
        return getByIdForUser(id, user.getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public R getByIdForUser(final long id, final String username) throws NotFoundException {
        return getDao().findByIdAndUsername(id, username).orElseThrow(() -> new NotFoundException(getRequestType(), id));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<R> getAllOrderedByDate() {
        return getDao().findAllOrderedByDate();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<R> getAllOrderedByDate(final PaginatedPacsRequest request) {
        return getDao().findAllOrderedByDate(request);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public R getMostRecentForPacs(final long pacsId) {
        return instance(getDao().findByPacsIdOrderedByMostRecent(pacsId));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public R getMostRecentForStudyInstanceUid(final String studyInstanceUid) {
        return instance(getDao().findByStudyInstanceUidOrderedByMostRecent(studyInstanceUid));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> getAllWithOrder() {
        return _template.queryForList(QUERY_QUEUE_WITH_LOCATION, EmptySqlParameterSource.INSTANCE);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> getAllWithOrderForUser(final UserI user) {
        return getAllWithOrderForUser(user.getUsername());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<Map<String, Object>> getAllWithOrderForUser(final String username) {
        return _template.queryForList(QUERY_QUEUE_WITH_LOCATION_FOR_USER, new MapSqlParameterSource("user", username));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<R> getAllForPacsOrderedByPriorityAndDate(final long pacsId) {
        return getDao().findAllForPacsOrderedByPriorityAndDate(pacsId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<R> getAllForPacsOrderedByPriorityAndDate(final long pacsId, final PaginatedPacsRequest request) {
        return getDao().findAllForPacsOrderedByPriorityAndDate(pacsId);
    }
}
