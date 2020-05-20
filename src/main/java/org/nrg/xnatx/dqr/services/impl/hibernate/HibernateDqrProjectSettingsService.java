/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernateDqrProjectSettingsService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xnatx.dqr.domain.daos.DqrProjectSettingsDAO;
import org.nrg.xnatx.dqr.domain.entities.DqrProjectSettings;
import org.nrg.xnatx.dqr.services.DqrProjectSettingsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class HibernateDqrProjectSettingsService extends AbstractHibernateEntityService<DqrProjectSettings, DqrProjectSettingsDAO> implements DqrProjectSettingsService {
    @Autowired
    public HibernateDqrProjectSettingsService(final NamedParameterJdbcTemplate template) {
        _template = template;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DqrProjectSettings findSettingsByProject(final String projectId) throws NotFoundException {
        if (StringUtils.isBlank(projectId)) {
            return null;
        }
        if (!_template.queryForObject(QUERY_PROJECT_EXISTS, new MapSqlParameterSource("projectId", projectId), Boolean.class)) {
            throw new NotFoundException("Project " + projectId + " does not exist");
        }
        return getDao().getDqrAdminSettingsByProjectId(projectId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDqrEnabledForProject(final String projectId) throws NotFoundException {
        final DqrProjectSettings settings = findSettingsByProject(projectId);
        return settings != null && settings.isEnabled();
    }

    private static final String QUERY_PROJECT_EXISTS = "SELECT EXISTS(SELECT id FROM xnat_projectdata WHERE id = :projectId)";

    private final NamedParameterJdbcTemplate _template;
}
