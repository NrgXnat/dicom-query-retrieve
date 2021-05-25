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
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xapi.exceptions.NotModifiedException;
import org.nrg.xnatx.dqr.domain.daos.DqrProjectSettingsDAO;
import org.nrg.xnatx.dqr.domain.entities.DqrProjectSettings;
import org.nrg.xnatx.dqr.dto.DqrProjectSettingsDTO;
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
    public boolean isDqrConfigured(final String projectId) throws NotFoundException {
        final MapSqlParameterSource parameters = new MapSqlParameterSource("projectId", projectId);
        if (!_template.queryForObject(QUERY_PROJECT_EXISTS, parameters, Boolean.class)) {
            throw new NotFoundException("Project " + projectId + " does not exist");
        }
        return _template.queryForObject(QUERY_IS_DQR_CONFIGURED, parameters, Boolean.class);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public DqrProjectSettings getProjectSettings(final String projectId) throws NotFoundException {
        if (StringUtils.isBlank(projectId) || !isDqrConfigured(projectId)) {
            return null;
        }
        return getDao().findByProjectId(projectId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isDqrEnabledForProject(final String projectId) throws NotFoundException {
        final DqrProjectSettings settings = getProjectSettings(projectId);
        return settings != null && settings.isDqrEnabled();
    }

    @Override
    public DqrProjectSettings update(final DqrProjectSettingsDTO settings) throws NotFoundException, NotModifiedException, DataFormatException {
        final DqrProjectSettings persisted = getProjectSettings(settings.getProjectId());
        if (settings.getEnabled() == null || persisted.isDqrEnabled() == settings.getEnabled()) {
            throw new NotModifiedException("No changes were provided for project " + settings.getProjectId() + " for DQR settings");
        }
        persisted.setDqrEnabled(settings.getEnabled());
        update(persisted);
        return getProjectSettings(settings.getProjectId());
    }

    private static final String QUERY_PROJECT_EXISTS    = "SELECT EXISTS(SELECT id FROM xnat_projectdata WHERE id = :projectId)";
    private static final String QUERY_IS_DQR_CONFIGURED = "SELECT EXISTS(SELECT id FROM xhbm_dqr_project_settings WHERE project_id = :projectId)";

    private final NamedParameterJdbcTemplate _template;
}
