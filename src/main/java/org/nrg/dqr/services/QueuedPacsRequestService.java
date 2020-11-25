package org.nrg.dqr.services;

import org.nrg.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/19/18.
 */
public interface QueuedPacsRequestService extends BaseHibernateService<QueuedPacsRequest> {
    List<QueuedPacsRequest> getAllOrderedByDate();

    List<QueuedPacsRequest> getAllOrderedByDate(PaginatedPacsRequest request);

    List<QueuedPacsRequest> getAllForUser(UserI user);

    List<QueuedPacsRequest> getAllForUser(UserI user, PaginatedPacsRequest request);

    List<Map<String, Object>> getAllWithOrder();

    List<Map<String, Object>> getAllWithOrder(PaginatedPacsRequest request);

    List<Map<String, Object>> getAllWithOrderForUser(UserI user);

    List<Map<String, Object>> getAllWithOrderForUser(UserI user, PaginatedPacsRequest request);

    QueuedPacsRequest getByIdAndUser(long id, UserI user);

    List<QueuedPacsRequest> getAllForPacsOrderedByPriorityAndDate(long pacsId);

    List<QueuedPacsRequest> getAllForPacsOrderedByPriorityAndDate(long pacsId, PaginatedPacsRequest request);

    List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(long pacsId);

    List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(long pacsId, PaginatedPacsRequest request);
}
