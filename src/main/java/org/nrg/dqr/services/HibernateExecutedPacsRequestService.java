package org.nrg.dqr.services;

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.daos.ExecutedPacsRequestDAO;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
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
public class HibernateExecutedPacsRequestService extends AbstractHibernateEntityService<ExecutedPacsRequest, ExecutedPacsRequestDAO> implements ExecutedPacsRequestService {
    @Override
    @Transactional
    public List<ExecutedPacsRequest> getAllForUser(UserI user){
        return _dao.findAllForUser(user);
    }

    @Override
    @Transactional
    public ExecutedPacsRequest getByIdForUser(Long id, UserI user){
        List<ExecutedPacsRequest> list = _dao.findByIdForUser(id, user);
        if(list==null || list.size()==0){
            return null;
        }
        else{
            return list.get(0);
        }
    }

    @Inject
    private ExecutedPacsRequestDAO _dao;
}
