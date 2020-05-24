/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.ProjectIrbInfoDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbFile;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.springframework.stereotype.Repository;

import javax.annotation.Nonnull;
import java.util.List;

@Repository
public class ProjectIrbInfoDAO extends AbstractHibernateDAO<ProjectIrbInfo> {
    @Nonnull
    public ProjectIrbInfo findIrbInfoForProject(final String projectId) throws NotFoundException {
        final ProjectIrbInfo projectIrbInfo = instance(findByCriteria(Restrictions.eq("projectId", projectId)));
        if (projectIrbInfo == null) {
            throw new NotFoundException(projectId);
        }
        return projectIrbInfo;
    }

    public String findIrbNumberForProject(final String projectId) throws NotFoundException {
        return findIrbInfoForProject(projectId).getIrbNumber();
    }

    public List<ProjectIrbFile> findIrbFilesForProject(final String projectId) throws NotFoundException {
        return findIrbInfoForProject(projectId).getProjectIrbFiles();
    }

    public List<String> findIrbFileNamesForProject(final String projectId) throws NotFoundException {
        return checked(getSession().createQuery(HQL_IRB_FILE_NAMES).setParameter("projectId", projectId).list(), String.class);
    }

    private static final String HQL_IRB_FILE_NAMES = "SELECT f.irbFileName FROM IrbInfo i JOIN i.irbFiles f WHERE i.projectId = :projectId";
}
