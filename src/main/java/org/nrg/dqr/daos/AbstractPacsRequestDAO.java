/*
 * dicom-query-retrieve: org.nrg.dqr.daos.AbstractPacsRequestDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.daos;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.framework.ajax.PaginatedRequest;
import org.nrg.framework.ajax.hibernate.HibernateFilter;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xft.security.UserI;

import java.util.List;

public abstract class AbstractPacsRequestDAO<E extends PacsRequest> extends AbstractHibernateDAO<E> {
    public List<E> findAllOrderedByDate() {
        return findAllOrderedByDate(new PaginatedPacsRequest());
    }

    public List<E> findAllOrderedByDate(final PaginatedPacsRequest request) {
        return findPaginated(ObjectUtils.defaultIfNull(request, new PaginatedPacsRequest()).toBuilder().clearSortBys()
                                        .sortColumn("queuedTime")
                                        .sortDir(PaginatedRequest.SortDir.ASC).build());
    }

    public List<E> findAllByUser(final UserI user) {
        return findAllByUser(user, null);
    }

    public List<E> findAllByUser(final UserI user, final PaginatedPacsRequest request) {
        return findPaginated(ObjectUtils.defaultIfNull(request, new PaginatedPacsRequest()).toBuilder().clearFiltersMap()
                                        .filter("username", HibernateFilter.builder().operator(HibernateFilter.Operator.EQ).value(user.getUsername()).build())
                                        .build());
    }

    public List<E> findAllByIdAndUser(final long id, final UserI user) {
        return findAllByIdAndUser(id, user, null);
    }

    public List<E> findAllByIdAndUser(final long id, final UserI user, final PaginatedPacsRequest request) {
        final PaginatedPacsRequest realized = ObjectUtils.defaultIfNull(request, new PaginatedPacsRequest());
        realized.getFiltersMap().put("id", HibernateFilter.builder().operator(HibernateFilter.Operator.EQ).value(id).build());
        realized.getFiltersMap().put("username", HibernateFilter.builder().operator(HibernateFilter.Operator.EQ).value(user.getUsername()).build());
        return findPaginated(realized);
    }

    public List<E> findByPacsIdOrderedByMostRecent(final long pacsId) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.addOrder(Order.desc("executedTime"));
        return GenericUtils.convertToTypedList(criteria.list(), getParameterizedType());
    }

    public List<E> findByStudyInstanceUidOrderedByMostRecent(final String studyInstanceUid) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("studyInstanceUid", studyInstanceUid));
        criteria.addOrder(Order.desc("executedTime"));
        return GenericUtils.convertToTypedList(criteria.list(), getParameterizedType());
    }

    public List<E> findByPacsIdForUser(final long pacsId, final UserI user) {
        return findByPacsIdForUser(pacsId, user, new PaginatedPacsRequest());
    }

    public List<E> findByPacsIdForUser(final long pacsId, final UserI user, final PaginatedPacsRequest request) {
        return findPaginated(ObjectUtils.defaultIfNull(request, new PaginatedPacsRequest()).toBuilder().clearFiltersMap()
                                        .filter("pacsId", HibernateFilter.builder().operator(HibernateFilter.Operator.EQ).value(pacsId).build())
                                        .filter("username", HibernateFilter.builder().operator(HibernateFilter.Operator.EQ).value(user.getUsername()).build()).build());
    }

    public List<E> findAllForPacsOrderedByPriorityAndDate(final long pacsId) {
        return findAllForPacsOrderedByPriorityAndDate(pacsId, new PaginatedPacsRequest());
    }

    public List<E> findAllForPacsOrderedByPriorityAndDate(final long pacsId, final PaginatedPacsRequest request) {
        return findPaginated(ObjectUtils.defaultIfNull(request, new PaginatedPacsRequest()).toBuilder().clearFiltersMap().clearSortBys()
                                        .filter("pacsId", HibernateFilter.builder().operator(HibernateFilter.Operator.EQ).value(pacsId).build())
                                        .sortBy(Pair.of("priority", PaginatedRequest.SortDir.ASC))
                                        .sortBy(Pair.of("queuedTime", PaginatedRequest.SortDir.ASC)).build());
    }
}
