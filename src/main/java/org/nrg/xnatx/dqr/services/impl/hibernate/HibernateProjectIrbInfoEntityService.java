/*
 * org.nrg.xnatx.dqr.services.impl.hibernate.HibernatePacsEntityService
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

import org.nrg.xnatx.dqr.domain.daos.ProjectIrbInfoDAO;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.services.ProjectIrbInfoEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class HibernateProjectIrbInfoEntityService extends AbstractHibernateEntityService<ProjectIrbInfo, ProjectIrbInfoDAO> implements ProjectIrbInfoEntityService {

    @Override
    @Transactional
    public ProjectIrbInfo findIrbInfoForProject(String projectId){
        return getDao().findIrbInfoForProject(projectId);
    }

    @Override
    @Transactional
    public String findIrbNumberForProject(String projectId){
        return getDao().findIrbNumberForProject(projectId);
    }

    @Override
    @Transactional
    public byte[] findIrbFileForProject(String projectId){
        return getDao().findIrbFileForProject(projectId);
    }

}
