package org.nrg.dqr.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Repository;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class QueuedPacsRequestDAO extends AbstractHibernateDAO<QueuedPacsRequest> {

    public List<QueuedPacsRequest> findAllOrderedByDate(){
        Criteria criteria = getCriteriaForType();
        criteria.addOrder(Order.asc("queuedTime"));
        if (criteria.list().size() == 0) {
            return null;
        }
        return criteria.list();
    }

    public List<QueuedPacsRequest> findAllForUser(UserI user){
        return findByCriteria(Restrictions.eq("username", user.getUsername()));
    }

    public List<QueuedPacsRequest> findByIdForUser(Long id, UserI user){
        return findByCriteria(Restrictions.eq("id", id),Restrictions.eq("username", user.getUsername()));
    }

    public List<QueuedPacsRequest> findAllForPacsOrderedByPriorityAndDate(Long pacsId){
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.addOrder(Order.asc("priority"));
        criteria.addOrder(Order.asc("queuedTime"));
        return criteria.list();
    }

    public List<QueuedPacsRequest> findQueuedOrFailedForPacsOrderedByPriorityAndDate(Long pacsId){
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.add(Restrictions.in("status", new String[]{PacsRequest.FAILED_STATUS_TEXT, PacsRequest.QUEUED_STATUS_TEXT}));
        criteria.addOrder(Order.asc("priority"));
        criteria.addOrder(Order.asc("queuedTime"));
        return criteria.list();
    }
}
