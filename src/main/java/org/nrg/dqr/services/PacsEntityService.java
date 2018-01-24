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

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.dqr.domain.entities.Pacs;

import java.util.List;

public interface PacsEntityService extends BaseHibernateService<Pacs> {

    public List<Pacs> findAllStorable();

    public List<Pacs> findAllQueryable();

    public boolean isAvailable(final Pacs entity);
}
