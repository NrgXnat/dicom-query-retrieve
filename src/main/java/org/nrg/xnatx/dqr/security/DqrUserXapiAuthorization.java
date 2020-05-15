/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.security.DqrUserXapiAuthorization
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.security;

import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.servlet.http.HttpServletRequest;
import org.apache.commons.lang3.StringUtils;
import org.aspectj.lang.JoinPoint;
import org.nrg.prefs.events.PreferenceHandlerMethod;
import org.nrg.xapi.authorization.AbstractXapiAuthorization;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.helpers.AccessLevel;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.event.listeners.methods.AbstractXnatPreferenceHandlerMethod;
import org.nrg.xnatx.dqr.dto.PacsImportRequest;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Checks whether user can access Dqr features.
 */
@Component
public class DqrUserXapiAuthorization extends AbstractXapiAuthorization implements PreferenceHandlerMethod {
    @Autowired
    public DqrUserXapiAuthorization(final DqrPreferences preferences, final NamedParameterJdbcTemplate template) {
        _allowAllUsersToUseDqr = new AtomicBoolean(preferences.getAllowAllUsersToUseDqr());
        _template = template;
    }

    /**
     * Tests whether the current user should be able to access any API calls that specify this authorization delegate. If
     * {@link DqrPreferences#getAllowAllUsersToUseDqr()} is not true, the user must be an admin or have the Dqr role.
     */
    protected boolean checkImpl(final AccessLevel accessLevel, final JoinPoint joinPoint, final UserI user, final HttpServletRequest request) throws InsufficientPrivilegesException {
        // If the user's a site or data admin or data access user, they can do whatever.
        if (Roles.isSiteAdmin(user) || ((XDATUser) user).isDataAccess()) {
            return true;
        }
        // If not all users can use DQR and this user doesn't have the Dqr role, they can't do whatever it is they're trying to do.
        if (!_allowAllUsersToUseDqr.get() && !Roles.checkRole(user, "Dqr")) {
            return false;
        }
        // Are they trying to act on particular projects, subjects, experiments?
        final Set<String>                       projects       = new HashSet<>(getProjects(joinPoint));
        final List<? extends PacsImportRequest> importRequests = getParameters(joinPoint, PacsImportRequest.class);
        if (!importRequests.isEmpty()) {
            projects.addAll(importRequests.stream().map(PacsImportRequest::getProject).filter(Objects::nonNull).collect(Collectors.toList()));
        }
        final List<String> experiments = getExperiments(joinPoint);
        // None found, so user can proceed
        final boolean hasProjects    = !projects.isEmpty();
        final boolean hasExperiments = !experiments.isEmpty();
        if (!hasProjects && !hasExperiments) {
            return true;
        }
        if (hasProjects && projects.size() > 1) {
            throw new InsufficientPrivilegesException("Can't operate on multiple projects at once");
        }
        final boolean               isRead     = StringUtils.equalsIgnoreCase("GET", request.getMethod());
        final MapSqlParameterSource parameters = new MapSqlParameterSource("username", user.getUsername()).addValue("action", isRead ? "read" : "edit");
        if (hasExperiments && !hasProjects) {
            final List<String> forbidden = experiments.stream().filter(experiment -> !_template.queryForObject(QUERY_USER_CAN_ACTION_ENTITY, parameters.addValue("experiment", experiment), Boolean.class)).collect(Collectors.toList());
            if (!forbidden.isEmpty()) {
                throw new InsufficientPrivilegesException(user.getUsername(), forbidden);
            }
            return true;
        }
        final String project = projects.iterator().next();
        parameters.addValue("project", project);
        final List<String> forbidden = experiments.stream().filter(experiment -> !_template.queryForObject(QUERY_USER_CAN_ACTION_ENTITY_IN_PROJECT, parameters.addValue("experiment", experiment), Boolean.class)).collect(Collectors.toList());
        if (!forbidden.isEmpty()) {
            throw new InsufficientPrivilegesException(user.getUsername(), project, forbidden);
        }
        return true;
    }

    @Override
    protected boolean considerGuests() {
        return false;
    }

    @Override
    public List<String> getToolIds() {
        return _handlerProxy.getToolIds();
    }

    @Override
    public List<String> getHandledPreferences() {
        return _handlerProxy.getHandledPreferences();
    }

    @Override
    public Set<String> findHandledPreferences(final Collection<String> preferences) {
        return _handlerProxy.findHandledPreferences(preferences);
    }

    @Override
    public void handlePreferences(final Map<String, String> values) {
        _handlerProxy.handlePreferences(values);
    }

    @Override
    public void handlePreference(final String preference, final String value) {
        _handlerProxy.handlePreference(preference, value);
    }

    private final PreferenceHandlerMethod _handlerProxy = new AbstractXnatPreferenceHandlerMethod(DqrPreferences.DQR_TOOL_ID, "allowAllUsersToUseDqr") {
        @Override
        protected void handlePreferenceImpl(final String preference, final String value) {
            _allowAllUsersToUseDqr.set(Boolean.parseBoolean(value));
        }
    };

    private static final String QUERY_USER_CAN_ACTION_ENTITY            = "SELECT * FROM data_type_fns_can_action_entity(:username, :action, :experiment)";
    private static final String QUERY_USER_CAN_ACTION_ENTITY_IN_PROJECT = "SELECT * FROM data_type_fns_can(:username, :action, :experiment, :project)";

    private final NamedParameterJdbcTemplate _template;
    private final AtomicBoolean              _allowAllUsersToUseDqr;
}
