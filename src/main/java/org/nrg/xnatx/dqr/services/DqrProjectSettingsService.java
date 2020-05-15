/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.DqrProjectSettingsService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xnatx.dqr.domain.entities.DqrProjectSettings;

/**
 * The Interface DqrProjectSettingsService.
 */
public interface DqrProjectSettingsService extends BaseHibernateService<DqrProjectSettings> {
    /**
     * Gets the stored DQR settings for the specified project.
     *
     * @param projectId The project for which settings should be retrieved.
     *
     * @return Returns the DQR settings for the specified project or <b>null</b> if the project ID is blank or the project has no associated DQR settings.
     *
     * @throws NotFoundException When the specified project doesn't exist.
     */
    DqrProjectSettings findSettingsByProject(final String projectId) throws NotFoundException;

    /**
     * Gets the DQR enabled setting for the specified project.
     *
     * @param projectId The project for which the DQR enabled settings should be retrieved.
     *
     * @return Returns the DQR enabled setting for the specified project or <b>false</b> if the project ID is blank or the project has no associated DQR settings.
     *
     * @throws NotFoundException When the specified project doesn't exist.
     */
    boolean isDqrEnabledForProject(final String projectId) throws NotFoundException;
}
