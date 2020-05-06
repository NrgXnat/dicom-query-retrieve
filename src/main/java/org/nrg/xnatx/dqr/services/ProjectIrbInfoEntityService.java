/*
 * org.nrg.xnatx.dqr.services.PacsEntityService
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.services;

import org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

public interface ProjectIrbInfoEntityService extends BaseHibernateService<ProjectIrbInfo> {

    public ProjectIrbInfo findIrbInfoForProject(String projectId);

    public String findIrbNumberForProject(String projectId);

    public byte[] findIrbFileForProject(String projectId);

}
