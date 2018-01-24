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

import java.time.LocalTime;
import java.util.List;

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
        LocalTime currentTime = LocalTime.now();
        String availabilityStartTimeString = entity.getAvailabilityStart();
        String availabilityEndTimeString = entity.getAvailabilityEnd();
        if(StringUtils.isBlank(availabilityStartTimeString) || StringUtils.isBlank(availabilityStartTimeString)) {
            pacsIsAvailable = true; //If time constraints are not set for the PACS, allow access
        }
        else{
            LocalTime availabilityStartTime = LocalTime.parse(availabilityStartTimeString);
            LocalTime availabilityEndTime = LocalTime.parse(availabilityEndTimeString);
            if (availabilityStartTime == null || availabilityEndTime == null) {
                pacsIsAvailable = true; //If time constraints are not set for the PACS, allow access
            }
            else{
                if(availabilityEndTime.isBefore(availabilityStartTime)){
                    //That means that the availability interval contains midnight.
                    if(currentTime.isAfter(availabilityStartTime) || currentTime.isBefore(availabilityEndTime)){
                        pacsIsAvailable = true;
                    }
                }
                else{
                    if(currentTime.isAfter(availabilityStartTime) && currentTime.isBefore(availabilityEndTime)){
                        pacsIsAvailable = true;
                    }
                }
            }
        }
        return pacsIsAvailable;
    }
}
