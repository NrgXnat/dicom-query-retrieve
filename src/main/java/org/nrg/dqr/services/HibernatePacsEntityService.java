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
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.dqr.daos.PacsDAO;
import org.nrg.dqr.domain.entities.Pacs;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Time;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

@Service
public class HibernatePacsEntityService extends AbstractHibernateEntityService<Pacs, PacsDAO> implements PacsEntityService {

    @Override
    @Transactional
    public Pacs create(final Pacs entity) {
        clearDefaultPacsFlagsOnOtherEntitiesIfThisEntityIsTheNewDefault(entity);
        return super.create(entity);
    }

    @Override
    @Transactional
    public void update(final Pacs entity) {
        clearDefaultPacsFlagsOnOtherEntitiesIfThisEntityIsTheNewDefault(entity);
        super.update(entity);
    }

    private void clearDefaultPacsFlagsOnOtherEntitiesIfThisEntityIsTheNewDefault(final Pacs entity) {
        if (entity.isDefaultStoragePacs() || entity.isDefaultQueryRetrievePacs()) {
            // Hibernate barfs if you load the same entity twice in a session, even if you only update it once.
            // So we have to exclude the entity in question here.
            final List<Pacs> allPacs = getDao().findAllBut(entity);
            for (final Pacs p : allPacs) {
                if (p.getId() != entity.getId()) {
                    if (entity.isDefaultStoragePacs()) {
                        p.setDefaultStoragePacs(false);
                    }
                    if (entity.isDefaultQueryRetrievePacs()) {
                        p.setDefaultQueryRetrievePacs(false);
                    }
                    super.update(p);
                }
            }
        }
    }

    @Override
    @Transactional
    public List<Pacs> findAllQueryableAndStorable(){
        return getDao().findAllQueryableAndStorable();
    }

    @Override
    @Transactional
    public List<Pacs> findAllStorable(){
        return getDao().findAllStorable();
    }

    @Override
    @Transactional
    public List<Pacs> findAllQueryable(){
        return getDao().findAllQueryable();
    }

    @Override
    @Transactional
    public boolean isAvailable(final Pacs entity){
        boolean pacsIsAvailable = false;
        String availabilityStartTimeString = entity.getAvailabilityStart();
        String availabilityEndTimeString = entity.getAvailabilityEnd();
        if(StringUtils.isBlank(availabilityStartTimeString) || StringUtils.isBlank(availabilityStartTimeString)) {
            pacsIsAvailable = true; //If time constraints are not set for the PACS, allow access
        }
        else{

            //If hour is one digit, pad with a zero.
            if(availabilityStartTimeString.charAt(1)==':'){
                availabilityStartTimeString = "0"+availabilityStartTimeString;
            }
            if(availabilityEndTimeString.charAt(1)==':'){
                availabilityEndTimeString = "0"+availabilityEndTimeString;
            }
            Calendar currentCal = Calendar.getInstance();

            long currMillis = currentCal.getTimeInMillis();
            long startMillis = 0L;
            long endMillis = 0L;

            if(StringUtils.isNotBlank(availabilityStartTimeString)){
                try {
                    Calendar startCal = (Calendar) currentCal.clone();
                    String[] startTime = StringUtils.split(availabilityStartTimeString, ":");
                    startCal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(startTime[0]));
                    startCal.set(Calendar.MINUTE,Integer.parseInt(startTime[1]));
                    startMillis = startCal.getTimeInMillis();
                }
                catch (Exception e){

                }
            }
            if(StringUtils.isNotBlank(availabilityEndTimeString)){
                try {
                    Calendar endCal = (Calendar) currentCal.clone();
                    String[] endTime = StringUtils.split(availabilityEndTimeString,":");
                    endCal.set(Calendar.HOUR_OF_DAY,Integer.parseInt(endTime[0]));
                    endCal.set(Calendar.MINUTE,Integer.parseInt(endTime[1]));
                    endMillis = endCal.getTimeInMillis();
                }
                catch (Exception e){

                }
            }

            if (startMillis == 0L || endMillis == 0L) {
                pacsIsAvailable = true; //If time constraints are not set for the PACS, allow access
            }
            else{
                if(endMillis<startMillis){
                    //That means that the availability interval contains midnight.
                    if(currMillis>startMillis || currMillis<endMillis){
                        pacsIsAvailable = true;
                    }
                }
                else{
                    if(currMillis>startMillis && currMillis<endMillis){
                        pacsIsAvailable = true;
                    }
                }
            }
        }
        return pacsIsAvailable;
    }
}
