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

package org.nrg.dqr.services;

import org.apache.commons.lang.StringUtils;
import org.nrg.dqr.daos.PacsAvailabilityDAO;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.PacsAvailability;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xdat.XDAT;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    public List<PacsAvailability> findSettingsByPacs(Long pacsId){
        return getDao().findSettingsByPacs(pacsId);
    }

    @Override
    @Transactional
    public List<PacsAvailability> findSettingsByPacsByDay(Long pacsId, int day){
        return getDao().findSettingsByPacsByDay(pacsId,day);
    }

    @Override
    @Transactional
    public Map<Integer, List<PacsAvailability>> findSettingsByPacsGroupedByDay(Long pacsId){
        Map<Integer, List<PacsAvailability>> availabilityByDay = new HashMap<>();
        for(int day=1;day<=7;day++) {
            availabilityByDay.put(day,getDao().findSettingsByPacsByDay(pacsId, day));
        }
        return availabilityByDay;
    }

    @Override
    @Transactional
    public Boolean checkOverlap(PacsAvailability availabilityToCheck, boolean removeOverlap){
        return checkOverlap(availabilityToCheck, removeOverlap, -1);
    }

    @Override
    @Transactional
    public Boolean checkOverlap(PacsAvailability availabilityToCheck, boolean removeOverlap, long existingIntervalId){
        Long pacsId = availabilityToCheck.getPacsId();
        int day = availabilityToCheck.getDayOfWeek();
        String startTimeString = availabilityToCheck.getAvailabilityStart();
        String endTimeString = availabilityToCheck.getAvailabilityEnd();
        List<PacsAvailability> intervals = getDao().findSettingsByPacsByDay(pacsId, day);
        long newStartMillis = 0L;
        long newEndMillis = 0L;

        Calendar currentCal = Calendar.getInstance();
        if (StringUtils.isNotBlank(startTimeString)) {
            try {
                Calendar startCal = (Calendar) currentCal.clone();
                String[] startTime = StringUtils.split(startTimeString, ":");
                startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTime[0]));
                startCal.set(Calendar.MINUTE, Integer.parseInt(startTime[1]));
                newStartMillis = startCal.getTimeInMillis();
            } catch (Exception e) {

            }
        }
        if (StringUtils.isNotBlank(endTimeString)) {
            try {
                Calendar endCal = (Calendar) currentCal.clone();
                String[] endTime = StringUtils.split(endTimeString, ":");
                endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTime[0]));
                endCal.set(Calendar.MINUTE, Integer.parseInt(endTime[1]));
                newEndMillis = endCal.getTimeInMillis();
            } catch (Exception e) {

            }
        }

        boolean hasOverlap = false;
        for (PacsAvailability availability : intervals) {
            long currAvailabilityId = availability.getId();
            String availabilityStartTimeString = availability.getAvailabilityStart();
            String availabilityEndTimeString = availability.getAvailabilityEnd();

            //If hour is one digit, pad with a zero.
            if (availabilityStartTimeString.charAt(1) == ':') {
                availabilityStartTimeString = "0" + availabilityStartTimeString;
            }
            if (availabilityEndTimeString.charAt(1) == ':') {
                availabilityEndTimeString = "0" + availabilityEndTimeString;
            }

            long oldStartMillis = 0L;
            long oldEndMillis = 0L;

            if (StringUtils.isNotBlank(availabilityStartTimeString)) {
                try {
                    Calendar startCal = (Calendar) currentCal.clone();
                    String[] startTime = StringUtils.split(availabilityStartTimeString, ":");
                    startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTime[0]));
                    startCal.set(Calendar.MINUTE, Integer.parseInt(startTime[1]));
                    oldStartMillis = startCal.getTimeInMillis();
                } catch (Exception e) {

                }
            }
            if (StringUtils.isNotBlank(availabilityEndTimeString)) {
                try {
                    Calendar endCal = (Calendar) currentCal.clone();
                    String[] endTime = StringUtils.split(availabilityEndTimeString, ":");
                    endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTime[0]));
                    endCal.set(Calendar.MINUTE, Integer.parseInt(endTime[1]));
                    oldEndMillis = endCal.getTimeInMillis();
                } catch (Exception e) {

                }
            }

            if(newStartMillis<=oldStartMillis && oldStartMillis<newEndMillis){
                hasOverlap = true;
                if(!removeOverlap) {
                    break;
                }
                else if(existingIntervalId!=currAvailabilityId){
                    if(newEndMillis>=oldEndMillis){
                        //case 4
                        delete(currAvailabilityId);
                    }
                    else{
                        //case 1
                        availability.setAvailabilityStart(endTimeString);
                        update(availability);
                    }
                }
            }
            else if(newStartMillis>oldStartMillis && oldEndMillis>newEndMillis){
                hasOverlap = true;
                if(!removeOverlap) {
                    break;
                }
                else if(existingIntervalId!=currAvailabilityId){
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
            }
            else if(newStartMillis<oldEndMillis && oldEndMillis<=newEndMillis){
                hasOverlap = true;
                if(!removeOverlap) {
                    break;
                }
                else if(existingIntervalId!=currAvailabilityId){
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
    public void deleteAllForPacs(Long pacsId){
        final List<PacsAvailability> pacsAvailabilities = getDao().findSettingsByPacs(pacsId);
        for (final PacsAvailability p : pacsAvailabilities) {
            delete(p);
        }
    }

}
