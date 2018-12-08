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

}
