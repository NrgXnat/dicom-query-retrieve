/*
 * org.nrg.xnatx.dqr.services.PacsEntityService
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

import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

public interface PacsAvailabilityEntityService extends BaseHibernateService<PacsAvailability> {
    List<PacsAvailability> findSettingsByPacs(Long pacsId);

    List<PacsAvailability> findSettingsByPacsByDay(Long pacsId, int day);

    Map<Integer, List<PacsAvailability>> findSettingsByPacsGroupedByDay(Long pacsId);

    @Nullable
    PacsAvailability findAvailableNow(final long pacsId);

    Boolean checkOverlap(PacsAvailability availability, boolean removeOverlap);

    Boolean checkOverlap(PacsAvailability availability, boolean removeOverlap, long existingIntervalId);

    void deleteAllForPacs(Long pacsId);
}
