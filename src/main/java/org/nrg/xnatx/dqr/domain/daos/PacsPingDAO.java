package org.nrg.xnatx.dqr.domain.daos;

import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.PacsPing;
import org.springframework.stereotype.Repository;

/**
 * Created by mike on 1/19/18.
 */
@Repository
public class PacsPingDAO extends AbstractHibernateDAO<PacsPing> {
    public PacsPing getLatestPing(final long pacsId) {
        final Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.addOrder(Order.desc("pingTime"));
        criteria.setMaxResults(1);
        return instance(checked(criteria.list()));
    }

    public List<PacsPing> getPings(final long pacsId) {
        return findByCriteria(Restrictions.eq("pacsId", pacsId));
    }
}
