/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.domain.daos;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.ajax.PaginatedRequest;
import org.nrg.framework.ajax.hibernate.HibernateFilter;
import org.nrg.framework.generics.GenericUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalRequest;
import org.springframework.stereotype.Repository;

import javax.annotation.Nullable;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class SeriesRetrievalRequestDAO extends AbstractHibernateDAO<SeriesRetrievalRequest> {
    /**
     * Find all series requests for the named user into the named project.
     * @param studyInstanceUid study containing the requested series
     * @param seriesInstanceUid requested series
     * @param project XNAT project to which series is to be downloaded
     * @param username requesting user: if null, retrieve matching requests for all users
     * @return List of matching requests; if user has repeated the request, may include duplicate entries
     * with different creation times
     */
    public List<SeriesRetrievalRequest> findByStudySeriesProjectUsername(
            final String studyInstanceUid,
            final String seriesInstanceUid,
            final String project,
            final @Nullable String username
            ) {
        final Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("studyInstanceUid", studyInstanceUid));
        criteria.add(Restrictions.eq("seriesInstanceUid", seriesInstanceUid));
        criteria.add(Restrictions.eq("destinationProject", project));
        if (!StringUtils.isBlank(username)) {
            criteria.add(Restrictions.eq("requestingUser", username));
        }
        return GenericUtils.convertToTypedList(criteria.list(), getParameterizedType());
    }

    /**
     * Has a series in the named study been requested for download into the named project?
     * @param studyInstanceUid study containing requested series
     * @param project XNAT project into which series has been requested
     * @param username requesting user; if null, check for requests from any user
     * @return true if any series in the named study has been requested for the named project
     */
    public boolean hasBeenRequested(
            final String studyInstanceUid,
            final String project,
            final @Nullable String username
    ) {
        final Map<String,Object> params = new LinkedHashMap<>();
        params.put("studyInstanceUid", studyInstanceUid);
        params.put("destinationProject", project);
        if (!StringUtils.isBlank(username)) {
            params.put("requestingUser", username);
        }
        return exists(params);
    }

    /**
     * Find all series requests for the named user, newest first.
     * @param username requesting user; if null, return all series requests
     * @param request pagination parameters
     * @return List of matching series requests, newest first.
     */
    public List<SeriesRetrievalRequest> findReverseChronological(final @Nullable String username, final PaginatedPacsRequest request) {
        final PaginatedPacsRequest.PaginatedPacsRequestBuilder builder = ObjectUtils.defaultIfNull(request, new PaginatedPacsRequest())
                .toBuilder()
                .clearFiltersMap().clearSortBys();
        if (null != username) {
            builder.filter("requestingUser", HibernateFilter.builder()
                    .operator(HibernateFilter.Operator.EQ).value(username).build());
        }
        //noinspection unchecked
        builder.sortBy(Pair.of("created", PaginatedRequest.SortDir.DESC));
        return findPaginated(builder.build());
    }
}
