/*
 * org.nrg.dqr.services.HibernatePacsEntityService
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.apache.commons.lang3.ObjectUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.domain.daos.PacsAvailabilityDAO;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.xnatx.dqr.services.PacsAvailabilityService;
import org.nrg.xnatx.dqr.utils.DqrDateRange;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Nullable;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Transactional
public class HibernatePacsAvailabilityService extends AbstractHibernateEntityService<PacsAvailability, PacsAvailabilityDAO> implements PacsAvailabilityService {
    @Override
    public PacsAvailability create(final PacsAvailability entity) {
        return super.create(entity);
    }

    @Override
    public void update(final PacsAvailability entity) {
        super.update(entity);
    }

    @Override
    public List<PacsAvailability> findAllByPacsId(final long pacsId) {
        return getDao().findAllByPacsId(pacsId);
    }

    @Override
    public List<PacsAvailability> findAllByPacsIdAndDayOfWeek(final long pacsId, final DayOfWeek day) {
        return getDao().findAllByPacsIdAndDayOfWeek(pacsId, day);
    }

    @Override
    public Map<DayOfWeek, List<PacsAvailability>> findAllByPacsIdGroupedByDayOfWeek(final long pacsId) {
        return Arrays.stream(DayOfWeek.values()).collect(Collectors.toMap(Function.identity(), dayOfWeek -> getDao().findAllByPacsIdAndDayOfWeek(pacsId, dayOfWeek)));
    }

    @Override
    @Nullable
    public Optional<PacsAvailability> findAvailableNow(final long pacsId) {
        return findAllByPacsIdAndDayOfWeek(pacsId, LocalDate.now().getDayOfWeek()).stream().filter(PacsAvailability::isAvailableNow).findAny();
    }

    @Override
    @Transactional
    public boolean checkOverlap(PacsAvailability availabilityToCheck, boolean removeOverlap) {
        return checkOverlap(availabilityToCheck, removeOverlap, -1);
    }

    @Override
    public boolean checkOverlap(final PacsAvailability reference, final boolean removeOverlap, final long existingIntervalId) {
        final long      pacsId = reference.getPacsId();
        final DayOfWeek day    = reference.getDayOfWeek();

        final Map<DqrDateRange.Relative, List<PacsAvailability>> segregated = getDao().findAllByPacsIdAndDayOfWeek(pacsId, day).stream().filter(availability -> availability.getId() != reference.getId()).collect(Collectors.groupingBy(reference::relative));

        final boolean hasOverlap = !Collections.disjoint(segregated.keySet(), OVERLAPS);
        if (!hasOverlap || !removeOverlap) {
            return hasOverlap;
        }

        // Delete all existing availabilities that are identical to or included by the reference availability.
        final List<PacsAvailability> identical     = ObjectUtils.defaultIfNull(segregated.get(DqrDateRange.Relative.Identical), Collections.emptyList());
        final List<PacsAvailability> includes      = ObjectUtils.defaultIfNull(segregated.get(DqrDateRange.Relative.Includes), Collections.emptyList());
        final List<PacsAvailability> included      = ObjectUtils.defaultIfNull(segregated.get(DqrDateRange.Relative.Included), Collections.emptyList());
        final List<PacsAvailability> beforeOverlap = ObjectUtils.defaultIfNull(segregated.get(DqrDateRange.Relative.BeforeOverlap), Collections.emptyList());
        final List<PacsAvailability> afterOverlap  = ObjectUtils.defaultIfNull(segregated.get(DqrDateRange.Relative.AfterOverlap), Collections.emptyList());
        Stream.concat(identical.stream(), includes.stream()).filter(availability -> availability.getId() != existingIntervalId).forEach(this::delete);
        included.stream().filter(availability -> availability.getId() != existingIntervalId).forEach(availability -> {
            create(PacsAvailability.builder().pacsId(pacsId).threads(availability.getThreads()).utilizationPercent(availability.getUtilizationPercent()).dayOfWeek(day).availabilityStart(reference.getAvailabilityEnd()).availabilityEnd(availability.getAvailabilityEnd()).build());
            availability.setAvailabilityEnd(reference.getAvailabilityStart());
            update(availability);
        });
        beforeOverlap.stream().filter(availability -> availability.getId() != existingIntervalId).forEach(availability -> {
            availability.setAvailabilityStart(reference.getAvailabilityEnd());
            update(availability);
        });
        afterOverlap.stream().filter(availability -> availability.getId() != existingIntervalId).forEach(availability -> {
            availability.setAvailabilityEnd(reference.getAvailabilityStart());
            update(availability);
        });

        return true;
    }

    @Override
    public void deleteAllByPacsId(final long pacsId) {
        getDao().findAllByPacsId(pacsId).forEach(this::delete);
    }

    private static final List<DqrDateRange.Relative> OVERLAPS = Arrays.asList(DqrDateRange.Relative.Identical, DqrDateRange.Relative.Included, DqrDateRange.Relative.Includes, DqrDateRange.Relative.BeforeOverlap, DqrDateRange.Relative.AfterOverlap);
}
