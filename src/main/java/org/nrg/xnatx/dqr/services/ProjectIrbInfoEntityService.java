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
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;

public interface ProjectIrbInfoEntityService extends BaseHibernateService<ProjectIrbInfo> {

    public ProjectIrbInfo findIrbInfoForProject(String projectId);

    public String findIrbNumberForProject(String projectId);

    public byte[] findIrbFileForProject(String projectId);

}
