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
    List<PacsAvailability> findSettingsByPacs(final long pacsId);

    List<PacsAvailability> findSettingsByPacsByDay(final long pacsId, final int day);

    Map<Integer, List<PacsAvailability>> findSettingsByPacsGroupedByDay(final long pacsId);

    @Nullable
    PacsAvailability findAvailableNow(final long pacsId);

    Boolean checkOverlap(final PacsAvailability availability, final boolean removeOverlap);

    Boolean checkOverlap(final PacsAvailability availability, final boolean removeOverlap, final long existingIntervalId);

    @SuppressWarnings("unused")
    void deleteAllForPacs(final long pacsId);
}
