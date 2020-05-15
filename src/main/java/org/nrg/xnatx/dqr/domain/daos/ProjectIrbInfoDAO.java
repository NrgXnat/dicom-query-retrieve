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
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectIrbInfoDAO extends AbstractHibernateDAO<ProjectIrbInfo> {
    public ProjectIrbInfo findIrbInfoForProject(final String projectId) {
        return instance(findByCriteria(Restrictions.eq("projectId", projectId)));
    }

    public String findIrbNumberForProject(final String projectId) {
        final ProjectIrbInfo irbInfo = findIrbInfoForProject(projectId);
        return irbInfo != null ? irbInfo.getIrbNumber() : null;
    }

    public byte[] findIrbFileForProject(final String projectId) {
        final ProjectIrbInfo irbInfo = findIrbInfoForProject(projectId);
        return irbInfo != null ? irbInfo.getIrbFile() : null;
    }
}
