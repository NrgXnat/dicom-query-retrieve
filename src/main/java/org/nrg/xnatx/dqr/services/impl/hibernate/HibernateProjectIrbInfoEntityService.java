/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernateProjectIrbInfoEntityService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.domain.daos.ProjectIrbInfoDAO;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.xnatx.dqr.services.ProjectIrbInfoEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateProjectIrbInfoEntityService extends AbstractHibernateEntityService<ProjectIrbInfo, ProjectIrbInfoDAO> implements ProjectIrbInfoEntityService {
    @Override
    public ProjectIrbInfo findIrbInfoForProject(final String projectId) {
        return getDao().findIrbInfoForProject(projectId);
    }

    @Override
    public String findIrbNumberForProject(final String projectId) {
        return getDao().findIrbNumberForProject(projectId);
    }

    @Override
    public byte[] findIrbFileForProject(final String projectId) {
        return getDao().findIrbFileForProject(projectId);
    }
}
