/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.AbstractPacsRequestDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.ajax.PaginatedRequest;
import org.nrg.framework.ajax.hibernate.HibernateFilter;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;

public abstract class AbstractPacsRequestDAO<E extends PacsRequest> extends AbstractHibernateDAO<E> {
    protected abstract String getTimeSortProperty();

    public List<E> findAllOrderedByDate() {
        return findAllOrderedByDate(getUnpaginatedRequest());
    }

    public List<E> findAllOrderedByDate(final @Nonnull PaginatedPacsRequest request) {
        final PaginatedPacsRequest sorted = request.toBuilder().sortBy(Pair.of( PaginatedRequest.SortDir.ASC,getTimeSortProperty())).build();
        return findPaginated(sorted);
    }

    public List<E> findAllByUsername(final String username) {
        return findAllByUsername(username, getUnpaginatedRequest());
    }

    public List<E> findAllByUsername(final String username, final @Nonnull PaginatedPacsRequest request) {
        return findPaginated(request.toBuilder().filter("username", getFilter(username)).build());
    }

    public Optional<E> findByIdAndUsername(final long id, final String username) {
        final E entity = findById(id);
        return entity == null || !StringUtils.equals(username, entity.getUsername()) ? Optional.empty() : Optional.of(entity);
    }

    public List<E> findByPacsIdOrderedByMostRecent(final long pacsId) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.addOrder(Order.desc(getTimeSortProperty()));
        return GenericUtils.convertToTypedList(criteria.list(), getParameterizedType());
    }

    public List<E> findByStudyInstanceUidOrderedByMostRecent(final String studyInstanceUid) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("studyInstanceUid", studyInstanceUid));
        criteria.addOrder(Order.desc(getTimeSortProperty()));
        return GenericUtils.convertToTypedList(criteria.list(), getParameterizedType());
    }

    @SuppressWarnings("unused")
    public List<E> findByPacsIdForUsername(final long pacsId, final String username) {
        return findByPacsIdForUsername(pacsId, username, getUnpaginatedRequest());
    }

    public List<E> findByPacsIdForUsername(final long pacsId, final String username, final @Nonnull PaginatedPacsRequest request) {
        return findPaginated(request.toBuilder()
                                    .clearFiltersMap()
                                    .filter("pacsId", getFilter(pacsId))
                                    .filter("username", getFilter(username)).build());
    }

    public List<E> findAllForPacsOrderedByPriorityAndDate(final long pacsId) {
        return findAllForPacsOrderedByPriorityAndDate(pacsId, getUnpaginatedRequest());
    }

    public List<E> findAllForPacsOrderedByPriorityAndDate(final long pacsId, final @Nonnull PaginatedPacsRequest request) {
        return findPaginated(request.toBuilder().clearFiltersMap().clearSortBys()
                                    .filter("pacsId", getFilter(pacsId))
                                    .sortBy(Pair.of(PaginatedRequest.SortDir.ASC, "priority"))
                                    .sortBy(Pair.of(PaginatedRequest.SortDir.ASC, getTimeSortProperty())).build());
    }

    public int deleteAllWithRequestIdAndStatus(final String requestId, final List<String> statuses) {
        getSession().getNamedNativeQuery(getParameterizedType().getSimpleName() + ".deleteSeriesIdsWithRequestIdAndStatus")
                .setParameter("requestId", requestId)
                .setParameterList("statuses", statuses)
                .executeUpdate();
        return getSession().getNamedNativeQuery(getParameterizedType().getSimpleName() + ".deleteAllWithRequestIdAndStatus")
                .setParameter("requestId", requestId)
                .setParameterList("statuses", statuses)
                .executeUpdate();
    }

    private static PaginatedPacsRequest getUnpaginatedRequest() {
        return PaginatedPacsRequest.builder().pageNumber(0).pageSize(0).build();
    }

    private static HibernateFilter getFilter(final @Nonnull Object item) {
        return HibernateFilter.builder().operator(HibernateFilter.Operator.EQ).value(item).build();
    }
}
