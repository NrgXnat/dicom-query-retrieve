/*
 * org.nrg.dqr.services.PacsEntityService
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

import org.nrg.dqr.domain.entities.PacsAvailability;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;

public interface PacsAvailabilityEntityService extends BaseHibernateService<PacsAvailability> {
    List<PacsAvailability> findSettingsByPacs(Long pacsId);
}
