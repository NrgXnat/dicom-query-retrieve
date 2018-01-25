package org.nrg.dqr.services;

import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
public interface QueuedPacsRequestService extends BaseHibernateService<QueuedPacsRequest> {
    List<QueuedPacsRequest> getAllOrderedByDate();
    List<QueuedPacsRequest> getAllForUser(UserI user);
    QueuedPacsRequest getByIdForUser(Long id, UserI user);
}
