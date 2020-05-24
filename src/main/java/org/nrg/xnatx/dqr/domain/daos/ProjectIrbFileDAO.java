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
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbFile;
import org.springframework.stereotype.Repository;

@Repository
public class ProjectIrbFileDAO extends AbstractHibernateDAO<ProjectIrbFile> {
}
