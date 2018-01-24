package org.nrg.dqr.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.springframework.stereotype.Repository;

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
}
