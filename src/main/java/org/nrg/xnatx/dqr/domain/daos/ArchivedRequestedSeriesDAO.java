/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.domain.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.ArchivedRequestedSeries;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public class ArchivedRequestedSeriesDAO extends AbstractHibernateDAO<ArchivedRequestedSeries> {
    /**
     * Retrieve the most recent update on contents of the named archived series.
     * @param studyInstanceUid study containing the series
     * @param seriesInstanceUid requested series
     * @param project XNAT project to which the series has been retrieved
     * @return most recent update if any, Optional::empty otherwise
     */
    public Optional<ArchivedRequestedSeries> latest(
            final String studyInstanceUid,
            final String seriesInstanceUid,
            final String project
    )  {
        final Criteria criteria = getCriteriaForType();
        criteria.add(Restrictions.eq("studyInstanceUid", studyInstanceUid));
        criteria.add(Restrictions.eq("seriesInstanceUid", seriesInstanceUid));
        criteria.add(Restrictions.eq("xnatProject", project));
        criteria.addOrder(Order.desc("created"));
        criteria.setFirstResult(0);
        criteria.setMaxResults(1);
        return Optional.ofNullable((ArchivedRequestedSeries) criteria.uniqueResult());
    }
}
