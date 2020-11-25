/*
 * org.nrg.dqr.services.PacsEntityService
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.services;

import org.nrg.dqr.domain.entities.PacsAvailability;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;

public interface PacsAvailabilityEntityService extends BaseHibernateService<PacsAvailability> {
    List<PacsAvailability> findAllByPacsId(long pacsId);

    List<PacsAvailability> findAllByPacsIdAndDayOfWeek(long pacsId, DayOfWeek day);

    Map<DayOfWeek, List<PacsAvailability>> findAllByPacsIdGroupedByDayOfWeek(long pacsId);

    boolean checkOverlap(PacsAvailability availability, boolean removeOverlap);

    boolean checkOverlap(PacsAvailability availability, boolean removeOverlap, final long existing);

    void deleteAllByPacsId(long pacsId);
}
