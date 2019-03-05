/*
 * PacsDAO
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.daos;

import org.hibernate.Criteria;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.PacsAvailability;
import org.nrg.dqr.services.PacsAvailabilityEntityService;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xdat.XDAT;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class PacsAvailabilityDAO extends AbstractHibernateDAO<PacsAvailability> {
    public List<PacsAvailability> findSettingsByPacs(Long pacsId) {
        return findByCriteria(Restrictions.eq("pacsId", pacsId));
    }

    public List<PacsAvailability> findSettingsByPacsByDay(Long pacsId, int day) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.add(Restrictions.eq("dayOfWeek", day));
        return criteria.list();
    }

}
