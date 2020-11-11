/*
 * web: org.nrg.xnat.node.services.impl.HibernateXnatNodeInfoService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2017, Washington University School of Medicine and Howard Hughes Medical Institute
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.dqr.services;

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.daos.DqrAdminSettingsForProjectDAO;
import org.nrg.dqr.domain.entities.DqrAdminSettingsForProject;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class HibernateDqrAdminSettingsForProjectService extends AbstractHibernateEntityService<DqrAdminSettingsForProject, DqrAdminSettingsForProjectDAO> implements DqrAdminSettingsForProjectService {
    @Override
    @Transactional
    public DqrAdminSettingsForProject findSettingsByProject(final String projectId) {
        return getDao().getDqrAdminSettingsByProjectId(projectId);
    }

    @Override
    @Transactional
    public boolean isDqrEnabledForProject(String projectId) {
        final DqrAdminSettingsForProject settings = findSettingsByProject(projectId);
        return settings != null && settings.isEnabled();
    }
}
