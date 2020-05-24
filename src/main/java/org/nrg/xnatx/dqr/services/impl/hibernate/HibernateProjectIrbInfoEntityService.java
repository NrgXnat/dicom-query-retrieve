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
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xnatx.dqr.domain.daos.ProjectIrbInfoDAO;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbFile;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.xnatx.dqr.services.ProjectIrbInfoEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@Transactional
public class HibernateProjectIrbInfoEntityService extends AbstractHibernateEntityService<ProjectIrbInfo, ProjectIrbInfoDAO> implements ProjectIrbInfoEntityService {
    @Override
    public ProjectIrbInfo findIrbInfoForProject(final String projectId) throws NotFoundException {
        return getDao().findIrbInfoForProject(projectId);
    }

    @Override
    public String findIrbNumberForProject(final String projectId) throws NotFoundException {
        return getDao().findIrbNumberForProject(projectId);
    }

    @Override
    public List<String> findIrbFileNamesForProject(final String projectId) throws NotFoundException {
        return getDao().findIrbFileNamesForProject(projectId);
    }

    @Override
    public List<ProjectIrbFile> findIrbFilesForProject(final String projectId) throws NotFoundException {
        return getDao().findIrbFilesForProject(projectId);
    }

    @Override
    public void addIrbFile(final ProjectIrbInfo info, final String fileName, final byte[] bytes) {
        info.addIrbFile(fileName, bytes);
        getDao().saveOrUpdate(info);
    }

    @Override
    public void createNewIrbInfo(final String projectId, final String fileName, final byte[] bytes) {
        final ProjectIrbInfo info = new ProjectIrbInfo();
        info.setProjectId(projectId);
        info.addIrbFile(fileName, bytes);
        getDao().saveOrUpdate(info);
    }
}
