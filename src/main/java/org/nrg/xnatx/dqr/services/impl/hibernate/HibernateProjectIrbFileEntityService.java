/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernateProjectIrbFileEntityService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.domain.daos.ProjectIrbFileDAO;
import org.nrg.xnatx.dqr.domain.entities.ProjectIrbFile;
import org.nrg.xnatx.dqr.services.ProjectIrbFileEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateProjectIrbFileEntityService extends AbstractHibernateEntityService<ProjectIrbFile, ProjectIrbFileDAO> implements ProjectIrbFileEntityService {
}
