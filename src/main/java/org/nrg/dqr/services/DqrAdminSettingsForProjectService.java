/*
 * web: org.nrg.xnat.node.services.XnatNodeInfoService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.services;

import org.nrg.dqr.domain.entities.DqrAdminSettingsForProject;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xnat.entities.ArchiveProcessorInstance;

import java.util.List;

/**
 * The Interface DqrAdminSettingsForProjectService.
 */
public interface DqrAdminSettingsForProjectService extends BaseHibernateService<DqrAdminSettingsForProject> {
//    List<ArchiveProcessorInstance> getAllSiteProcessors();
//    List<ArchiveProcessorInstance> getAllEnabledSiteProcessors();
//    List<ArchiveProcessorInstance> getAllEnabledSiteProcessorsForAe(String aeAndPort);
//    List<ArchiveProcessorInstance> getAllEnabledSiteProcessorsInOrder();
//    List<ArchiveProcessorInstance> getAllEnabledSiteProcessorsInOrderForLocation(final int location);

    DqrAdminSettingsForProject findSettingsByProject(final String projectId);
    boolean isDqrEnabledForProject(final String projectId);
}
