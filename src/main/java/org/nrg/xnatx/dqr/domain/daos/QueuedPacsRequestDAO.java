/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.QueuedPacsRequestDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.nrg.framework.ajax.PaginatedRequest;
import org.nrg.framework.ajax.hibernate.HibernateFilter;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.springframework.stereotype.Repository;

import java.util.List;

import static org.nrg.xnatx.dqr.domain.entities.PacsAvailability.PROP_PACS_ID;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class QueuedPacsRequestDAO extends AbstractPacsRequestDAO<QueuedPacsRequest> {
    @Override
    protected String getTimeSortProperty() {
        return "queuedTime";
    }

    public List<QueuedPacsRequest> findAllForUser(final UserI user) {
        return findByProperty("username", user.getUsername());
    }

    public List<QueuedPacsRequest> findQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId) {
        return findQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId, null);
    }

    public List<QueuedPacsRequest> findQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId, final PaginatedPacsRequest request) {
        return findPaginated(ObjectUtils.defaultIfNull(request, new PaginatedPacsRequest()).toBuilder().clearFiltersMap().clearSortBys()
                                        .filter(PROP_PACS_ID, HibernateFilter.builder().operator(HibernateFilter.Operator.EQ).value(pacsId).build())
                                        .filter("status", HibernateFilter.builder().operator(HibernateFilter.Operator.IN).values(FAILED_OR_QUEUED).build())
                                        .sortBy(Pair.of(PaginatedRequest.SortDir.ASC, "priority"))
                                        .sortBy(Pair.of(PaginatedRequest.SortDir.ASC, "queuedTime")).build());
    }

    private static final String[] FAILED_OR_QUEUED = {PacsRequest.FAILED_STATUS_TEXT, PacsRequest.QUEUED_STATUS_TEXT};
}
