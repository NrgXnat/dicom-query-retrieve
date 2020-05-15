/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernatePacsAvailabilityEntityService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.Nullable;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.domain.daos.PacsAvailabilityDAO;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.xnatx.dqr.services.PacsAvailabilityEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HibernatePacsAvailabilityEntityService extends AbstractHibernateEntityService<PacsAvailability, PacsAvailabilityDAO> implements PacsAvailabilityEntityService {

    @Override
    @Transactional
    public PacsAvailability create(final PacsAvailability entity) {
        return super.create(entity);
    }

    @Override
    @Transactional
    public void update(final PacsAvailability entity) {
        super.update(entity);
    }

    @Override
    @Transactional
    public List<PacsAvailability> findSettingsByPacs(final long pacsId) {
        return getDao().findSettingsByPacs(pacsId);
    }

    @Override
    @Transactional
    public List<PacsAvailability> findSettingsByPacsByDay(final long pacsId, final int day) {
        return getDao().findSettingsByPacsByDay(pacsId, day);
    }

    @Override
    @Transactional
    public Map<Integer, List<PacsAvailability>> findSettingsByPacsGroupedByDay(final long pacsId) {
        Map<Integer, List<PacsAvailability>> availabilityByDay = new HashMap<>();
        for (int day = 1; day <= 7; day++) {
            availabilityByDay.put(day, getDao().findSettingsByPacsByDay(pacsId, day));
        }
        return availabilityByDay;
    }

    @Override
    @Nullable
    public PacsAvailability findAvailableNow(final long pacsId) {
        final Calendar calendar = Calendar.getInstance();
        return findSettingsByPacsByDay(pacsId, calendar.get(Calendar.DAY_OF_WEEK)).stream().filter(availability -> availability.isAvailable(calendar)).findAny().orElse(null);
    }

    @Override
    @Transactional
    public Boolean checkOverlap(final PacsAvailability availabilityToCheck, final boolean removeOverlap) {
        return checkOverlap(availabilityToCheck, removeOverlap, -1);
    }

    @Override
    @Transactional
    public Boolean checkOverlap(final PacsAvailability availabilityToCheck, final boolean removeOverlap, final long existingIntervalId) {
        final long   pacsId          = availabilityToCheck.getPacsId();
        final int    day             = availabilityToCheck.getDayOfWeek();
        final String startTimeString = availabilityToCheck.getAvailabilityStart();
        final String endTimeString   = availabilityToCheck.getAvailabilityEnd();

        final Calendar calendar       = Calendar.getInstance();
        final long     newStartMillis = PacsAvailability.getAvailabilityTimeInMillis(calendar, availabilityToCheck.getAvailabilityStart());
        final long     newEndMillis   = PacsAvailability.getAvailabilityTimeInMillis(calendar, availabilityToCheck.getAvailabilityEnd());

        boolean hasOverlap = false;
        for (final PacsAvailability availability : getDao().findSettingsByPacsByDay(pacsId, day)) {
            final long currAvailabilityId = availability.getId();
            final long oldStartMillis     = PacsAvailability.getAvailabilityTimeInMillis(calendar, availability.getAvailabilityStart());
            final long oldEndMillis       = PacsAvailability.getAvailabilityTimeInMillis(calendar, availability.getAvailabilityEnd());

            if (newStartMillis <= oldStartMillis && oldStartMillis < newEndMillis) {
                hasOverlap = true;
                if (!removeOverlap) {
                    break;
                } else if (existingIntervalId != currAvailabilityId) {
                    if (newEndMillis >= oldEndMillis) {
                        //case 4
                        delete(currAvailabilityId);
                    } else {
                        //case 1
                        availability.setAvailabilityStart(endTimeString);
                        update(availability);
                    }
                }
            } else if (newStartMillis > oldStartMillis && oldEndMillis > newEndMillis) {
                hasOverlap = true;
                if (!removeOverlap) {
                    break;
                } else if (existingIntervalId != currAvailabilityId) {
                    //case 2
                    PacsAvailability newSecondHalfOfExisting = new PacsAvailability();
                    newSecondHalfOfExisting.setDayOfWeek(availability.getDayOfWeek());
                    newSecondHalfOfExisting.setPacsId(availability.getPacsId());
                    newSecondHalfOfExisting.setThreads(availability.getThreads());
                    newSecondHalfOfExisting.setUtilizationPercent(availability.getUtilizationPercent());
                    newSecondHalfOfExisting.setAvailabilityStart(endTimeString);
                    newSecondHalfOfExisting.setAvailabilityEnd(availability.getAvailabilityEnd());
                    create(newSecondHalfOfExisting);

                    availability.setAvailabilityEnd(startTimeString);
                    update(availability);
                }
            } else if (newStartMillis < oldEndMillis && oldEndMillis <= newEndMillis) {
                hasOverlap = true;
                if (!removeOverlap) {
                    break;
                } else if (existingIntervalId != currAvailabilityId) {
                    //case 3
                    availability.setAvailabilityEnd(startTimeString);
                    update(availability);
                }
            }
        }
        return hasOverlap;
    }

    @Override
    @Transactional
    public void deleteAllForPacs(final long pacsId) {
        for (final PacsAvailability p : getDao().findSettingsByPacs(pacsId)) {
            delete(p);
        }
    }
}
