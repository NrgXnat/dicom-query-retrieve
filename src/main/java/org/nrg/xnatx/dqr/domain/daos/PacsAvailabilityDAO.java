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

package org.nrg.xnatx.dqr.domain.daos;

import com.google.common.collect.ImmutableMap;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.springframework.stereotype.Repository;

import java.time.DayOfWeek;
import java.util.List;

import static org.nrg.xnatx.dqr.domain.entities.PacsAvailability.PROP_DAY_OF_WEEK;
import static org.nrg.xnatx.dqr.domain.entities.PacsAvailability.PROP_PACS_ID;

@Repository
public class PacsAvailabilityDAO extends AbstractHibernateDAO<PacsAvailability> {
    public List<PacsAvailability> findAllByPacsId(final long pacsId) {
        return findByProperty(PROP_PACS_ID, pacsId);
    }

    public List<PacsAvailability> findAllByPacsIdAndDayOfWeek(final long pacsId, final DayOfWeek day) {
        return findByProperties(ImmutableMap.of(PROP_PACS_ID, pacsId, PROP_DAY_OF_WEEK, day));
    }
}
