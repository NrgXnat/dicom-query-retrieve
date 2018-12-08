package org.nrg.dqr.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
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
public class HibernateExecutedPacsRequestService extends AbstractHibernateEntityService<ExecutedPacsRequest, ExecutedPacsRequestDAO> implements ExecutedPacsRequestService {

    private static final Log _log = LogFactory.getLog(HibernateExecutedPacsRequestService.class);

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

    @Override
    @Transactional
    public ExecutedPacsRequest getMostRecentForPacs(Long pacsId){
        List<ExecutedPacsRequest> list = _dao.findByPacsidOrderedByMostRecent(pacsId);
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
