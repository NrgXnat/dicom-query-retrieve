/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.ProjectIrbInfoDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xnat.entities.FileStoreInfo;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Repository
public class ProjectIrbInfoDAO extends AbstractHibernateDAO<ProjectIrbInfo> {
    @Nonnull
    public ProjectIrbInfo findIrbInfoForProject(final String projectId) throws NotFoundException {
        return Optional.ofNullable(findByUniqueProperty("projectId", projectId)).orElseThrow(() -> new NotFoundException(projectId));
    }

    public String findIrbNumberForProject(final String projectId) throws NotFoundException {
        return findIrbInfoForProject(projectId).getIrbNumber();
    }

    public List<FileStoreInfo> findIrbFilesForProject(final String projectId) throws NotFoundException {
        return findIrbInfoForProject(projectId).getProjectIrbFiles();
    }

    public List<String> findIrbFileNamesForProject(final String projectId) throws NotFoundException {
        return findIrbFilesForProject(projectId).stream().map(FileStoreInfo::getCoordinates).collect(Collectors.toList());
    }
}
