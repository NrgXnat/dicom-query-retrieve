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

import org.apache.commons.lang3.StringUtils;
import org.nrg.dqr.daos.ProjectIrbInfoDAO;
import org.nrg.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.sql.Blob;
import java.util.Calendar;
import java.util.List;

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
