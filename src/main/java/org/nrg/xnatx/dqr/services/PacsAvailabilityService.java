/*
 * org.nrg.dqr.services.PacsAvailabilityService
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.services;


import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;

import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.Optional;

public interface PacsAvailabilityService extends BaseHibernateService<PacsAvailability> {
    List<PacsAvailability> findAllByPacsId(long pacsId);

    List<PacsAvailability> findAllByPacsIdAndDayOfWeek(long pacsId, DayOfWeek day);

    Map<DayOfWeek, List<PacsAvailability>> findAllByPacsIdGroupedByDayOfWeek(long pacsId);

    Optional<PacsAvailability> findAvailableNow(final long pacsId);

    boolean checkOverlap(PacsAvailability availability, boolean removeOverlap);

    boolean checkOverlap(PacsAvailability availability, boolean removeOverlap, final long existing);

    void deleteAllByPacsId(long pacsId);
}
