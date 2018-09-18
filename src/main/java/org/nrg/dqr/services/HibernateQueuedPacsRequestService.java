package org.nrg.dqr.services;

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.daos.QueuedPacsRequestDAO;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
@Slf4j
public class HibernateQueuedPacsRequestService extends AbstractHibernateEntityService<QueuedPacsRequest, QueuedPacsRequestDAO> implements QueuedPacsRequestService {
    @Inject
    private QueuedPacsRequestDAO _dao;

    @Override
    @Transactional
    public List<QueuedPacsRequest> getAllOrderedByDate(){
        return getDao().findAllOrderedByDate();
    }

    @Override
    @Transactional
    public List<QueuedPacsRequest> getAllForUser(UserI user){
        return _dao.findAllForUser(user);
    }

    @Override
    @Transactional
    public QueuedPacsRequest getByIdForUser(Long id, UserI user){
        List<QueuedPacsRequest> list = _dao.findByIdForUser(id, user);
        if(list==null || list.size()==0){
            return null;
        }
        else{
            return list.get(0);
        }
    }
}
