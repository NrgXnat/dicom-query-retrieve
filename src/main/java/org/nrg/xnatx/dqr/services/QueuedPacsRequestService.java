package org.nrg.xnatx.dqr.services;

import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/19/18.
 */
public interface QueuedPacsRequestService extends BaseHibernateService<QueuedPacsRequest> {
    List<QueuedPacsRequest> getAllOrderedByDate();
    List<QueuedPacsRequest> getAllForUser(UserI user);
    List<Map<String, Object>> getAllWithOrder();
    List<Map<String, Object>> getAllWithOrderForUser(UserI user);
    QueuedPacsRequest getByIdForUser(Long id, UserI user);
    List<QueuedPacsRequest> getAllForPacsOrderedByPriorityAndDate(Long pacsId);
    List<QueuedPacsRequest> getQueuedOrFailedForPacsOrderedByPriorityAndDate(Long pacsId);
}
