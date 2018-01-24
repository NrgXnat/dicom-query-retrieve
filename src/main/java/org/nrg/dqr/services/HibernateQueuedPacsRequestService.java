package org.nrg.dqr.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.dqr.daos.ExecutedPacsRequestDAO;
import org.nrg.dqr.daos.QueuedPacsRequestDAO;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernateQueuedPacsRequestService extends AbstractHibernateEntityService<QueuedPacsRequest, QueuedPacsRequestDAO> implements QueuedPacsRequestService {

    private static final Log _log = LogFactory.getLog(HibernateQueuedPacsRequestService.class);

    @Inject
    private QueuedPacsRequestDAO _dao;

    @Override
    @Transactional
    public List<QueuedPacsRequest> getAllOrderedByDate(){
        return getDao().findAllOrderedByDate();
    }
}
