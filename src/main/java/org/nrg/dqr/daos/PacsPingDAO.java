package org.nrg.dqr.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.PacsPing;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xft.security.UserI;
import org.springframework.stereotype.Repository;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class PacsPingDAO extends AbstractHibernateDAO<PacsPing> {

    public PacsPing getLatestPing(Long pacsId){
        Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.addOrder(Order.desc("pingTime"));
        criteria.setMaxResults(1);
        if (criteria.list().size() == 0) {
            return null;
        }
        return (PacsPing)criteria.list().get(0);
    }

    public List<PacsPing> getPings(Long pacsId){
        return findByCriteria(Restrictions.eq("pacsId", pacsId));
    }
}
