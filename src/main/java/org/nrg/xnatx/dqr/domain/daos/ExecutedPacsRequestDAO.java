package org.nrg.xnatx.dqr.domain.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class ExecutedPacsRequestDAO extends AbstractHibernateDAO<ExecutedPacsRequest> {

    public List<ExecutedPacsRequest> findAllForUser(UserI user){
        return findByCriteria(Restrictions.eq("username", user.getUsername()));
    }

    public List<ExecutedPacsRequest> findByIdForUser(Long id, UserI user){
        return findByCriteria(Restrictions.eq("id", id),Restrictions.eq("username", user.getUsername()));
    }

    public List<ExecutedPacsRequest> findByPacsidOrderedByMostRecent(Long pacsId){
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.addOrder(Order.desc("executedTime"));
        return criteria.list();
    }

    public List<ExecutedPacsRequest> findByStudyInstanceUidOrderedByMostRecent(String studyInstanceUid){
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("studyInstanceUid", studyInstanceUid));
        criteria.addOrder(Order.desc("executedTime"));
        return criteria.list();
    }
}
