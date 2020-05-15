/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.PacsAvailabilityDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import java.util.List;
import org.hibernate.Criteria;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.springframework.stereotype.Repository;

@Repository
public class PacsAvailabilityDAO extends AbstractHibernateDAO<PacsAvailability> {
    public List<PacsAvailability> findSettingsByPacs(final long pacsId) {
        return findByCriteria(Restrictions.eq("pacsId", pacsId));
    }

    public List<PacsAvailability> findSettingsByPacsByDay(final long pacsId, final int day) {
        final Criteria criteria = getSession().createCriteria(getParameterizedType());
        criteria.add(Restrictions.eq("pacsId", pacsId));
        criteria.add(Restrictions.eq("dayOfWeek", day));
        return checked(criteria.list());
    }
}
