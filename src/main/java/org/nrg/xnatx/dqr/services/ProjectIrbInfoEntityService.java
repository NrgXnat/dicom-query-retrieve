/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.ProjectIrbInfoEntityService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.exceptions.ResourceAlreadyExistsException;
import org.nrg.xnat.entities.FileStoreInfo;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.springframework.core.io.Resource;

import java.util.Collection;
import java.util.List;

public interface ProjectIrbInfoEntityService extends BaseHibernateService<ProjectIrbInfo> {
    ProjectIrbInfo findIrbInfoForProject(final String projectId) throws NotFoundException;

    String findIrbNumberForProject(final String projectId) throws NotFoundException;

    List<String> findIrbFileNamesForProject(final String projectId) throws NotFoundException;

    List<FileStoreInfo> findIrbFilesForProject(final String projectId) throws NotFoundException;

    void addIrbFile(final ProjectIrbInfo info, final String fileName, final byte[] bytes) throws ResourceAlreadyExistsException;

    ProjectIrbInfo createNewIrbInfo(final String projectId, final String irbNumber, final String fileName, final byte[] bytes) throws ResourceAlreadyExistsException;

    ProjectIrbInfo createNewIrbInfo(final String projectId, final String irbNumber, final Resource... resources) throws ResourceAlreadyExistsException;

    ProjectIrbInfo createNewIrbInfo(final String projectId, final String irbNumber, final Collection<? extends Resource> resources) throws ResourceAlreadyExistsException;
}
