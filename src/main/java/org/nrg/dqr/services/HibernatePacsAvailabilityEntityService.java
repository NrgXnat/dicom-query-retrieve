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
import org.nrg.dqr.domain.entities.PacsAvailability;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Calendar;
import java.util.List;

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
    public boolean isAvailableByPacs(Long pacsId){





//        int currentSessionsPerHour = getDao().findCurrentSessionsPerHourByPacs(pacsId);
//
//
//
//        boolean pacsIsAvailable = false;
//        String availabilityStartTimeString = entity.getAvailabilityStart();
//        String availabilityEndTimeString = entity.getAvailabilityEnd();
//        if(StringUtils.isBlank(availabilityStartTimeString) || StringUtils.isBlank(availabilityStartTimeString)) {
//            pacsIsAvailable = true; //If time constraints are not set for the PACS, allow access
//        }
//        else{
//
//            //If hour is one digit, pad with a zero.
//            if(availabilityStartTimeString.charAt(1)==':'){
//                availabilityStartTimeString = "0"+availabilityStartTimeString;
//            }
//            if(availabilityEndTimeString.charAt(1)==':'){
//                availabilityEndTimeString = "0"+availabilityEndTimeString;
//            }
//            Calendar currentCal = Calendar.getInstance();
//
//            long currMillis = currentCal.getTimeInMillis();
//            long startMillis = 0L;
//            long endMillis = 0L;
//
//            if(StringUtils.isNotBlank(availabilityStartTimeString)){
//                try {
//                    Calendar startCal = (Calendar) currentCal.clone();
//                    String[] startTime = StringUtils.split(availabilityStartTimeString, ":");
//                    startCal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(startTime[0]));
//                    startCal.set(Calendar.MINUTE,Integer.parseInt(startTime[1]));
//                    startMillis = startCal.getTimeInMillis();
//                }
//                catch (Exception e){
//
//                }
//            }
//            if(StringUtils.isNotBlank(availabilityEndTimeString)){
//                try {
//                    Calendar endCal = (Calendar) currentCal.clone();
//                    String[] endTime = StringUtils.split(availabilityEndTimeString,":");
//                    endCal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(endTime[0]));
//                    endCal.set(Calendar.MINUTE,Integer.parseInt(endTime[1]));
//                    endMillis = endCal.getTimeInMillis();
//                }
//                catch (Exception e){
//
//                }
//            }
//
//            if (startMillis == 0L || endMillis == 0L) {
//                pacsIsAvailable = true; //If time constraints are not set for the PACS, allow access
//            }
//            else{
//                if(endMillis<startMillis){
//                    //That means that the availability interval contains midnight.
//                    if(currMillis>startMillis || currMillis<endMillis){
//                        pacsIsAvailable = true;
//                    }
//                }
//                else{
//                    if(currMillis>startMillis && currMillis<endMillis){
//                        pacsIsAvailable = true;
//                    }
//                }
//            }
//        }
//        return pacsIsAvailable;
return true;
    }

}
