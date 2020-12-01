package org.nrg.dqr.daos;

import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.ajax.PaginatedRequest;
import org.nrg.framework.ajax.hibernate.HibernateFilter;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class QueuedPacsRequestDAO extends AbstractPacsRequestDAO<QueuedPacsRequest> {
    public List<QueuedPacsRequest> findQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId) {
        return findQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId, null);
    }

    public List<QueuedPacsRequest> findQueuedOrFailedForPacsOrderedByPriorityAndDate(final long pacsId, final PaginatedPacsRequest request) {
        return findPaginated(ObjectUtils.defaultIfNull(request, new PaginatedPacsRequest()).toBuilder().clearFiltersMap().clearSortBys()
                                        .filter("pacsId", HibernateFilter.builder().operator(HibernateFilter.Operator.EQ).value(pacsId).build())
                                        .filter("status", HibernateFilter.builder().operator(HibernateFilter.Operator.IN).values(FAILED_OR_QUEUED).build())
                                        .sortBy(Pair.of("priority", PaginatedRequest.SortDir.ASC))
                                        .sortBy(Pair.of("queuedTime", PaginatedRequest.SortDir.ASC)).build());
    }

    private static final String[] FAILED_OR_QUEUED = {PacsRequest.FAILED_STATUS_TEXT, PacsRequest.QUEUED_STATUS_TEXT};
}
