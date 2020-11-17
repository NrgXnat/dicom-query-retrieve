/*
 * org.nrg.xnat.restlet.extensions.PacsServiceResource
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnat.restlet.extensions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.dqr.domain.DqrDomainObject;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.DqrAdminSettingsForProjectService;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.dqr.services.PacsService;
import org.nrg.dqr.services.QueuedPacsRequestService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.ResourceException;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;

import java.util.zip.DataFormatException;

@Getter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
@Slf4j
public abstract class PacsServiceResource extends PacsSerializingResource {
    public PacsServiceResource(final Context context, final Request request, final Response response) {
        super(context, request, response);

        getVariants().add(new Variant(MediaType.ALL));

        _pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        _pacsService = XDAT.getContextService().getBean(PacsService.class);
        _studyRoutingService = XDAT.getContextService().getBean(StudyRoutingService.class);
        _dqrPreferences = XDAT.getContextService().getBean(DqrPreferences.class);
        _dqrAdminSettings = XDAT.getContextService().getBean(DqrAdminSettingsForProjectService.class);
        _queuedPacsRequestService = XDAT.getContextService().getBean(QueuedPacsRequestService.class);
        _siteConfigPreferences = XDAT.getSiteConfigPreferences();
        _mailService = XDAT.getMailService();
    }

    protected abstract Representation representImpl(final Variant variant) throws ResourceException;

    @Override
    public Representation represent(final Variant variant) throws ResourceException {
        if (getUser().isGuest()) {
            respondWithNeedToBeLoggedIn();
            return null;
        }
        if (!Roles.checkRole(getUser(), "Administrator") && !Roles.checkRole(getUser(), "Dqr") && !getDqrPreferences().getAllowAllUsersToUseDqr()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You don't have permission to search the PACS.");
            return null;
        }
        return representImpl(variant);
    }

    public void respondWithNotFound() {
        respondWithNotFound("Nothing found that matches this request");
    }

    public void respondWithPacsNotFound() {
        respondWithNotFound("No PACS were found that match this request");
    }

    protected void respondWithInvalidPacsId(final String pacsId) {
        getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, "The PACS ID submitted is invalid: " + pacsId);
    }

    public void respondWithNotFound(final String message) {
        getResponse().setStatus(Status.CLIENT_ERROR_NOT_FOUND, message);
    }

    public void respondWithBadRequest(final String message) {
        getResponse().setStatus(Status.CLIENT_ERROR_BAD_REQUEST, message);
    }

    public void respondWithNeedToBeLoggedIn() {
        getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You must be logged in to query a PACS.");
    }

    protected Representation jsonRepresentation(final DqrDomainObject result, final Class<?> serializationView) {
        try {
            return new StringRepresentation(String.format(DOMAIN_OBJECT_FORMAT, writeValue(result, serializationView)), MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Study assignStudyToProject(final String projectId, final String studyInstanceUid) {
        if (!StringUtils.isBlank(projectId)) {
            log.debug("Assigning study instance UID {} to project {}", studyInstanceUid, projectId);
            _studyRoutingService.assign(studyInstanceUid, projectId, getUser().getLogin());
            return new Study(projectId, studyInstanceUid);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No project assignment specified for study instance UID " + studyInstanceUid + ", may be registered as Unassigned");
            }
            return new Study(studyInstanceUid);
        }
    }

    protected Representation jsonRepresentation(final PacsSearchResults<?, ?> results, final Class<?> serializationView) {
        try {
            return new StringRepresentation(String.format(SEARCH_RESULTS_FORMAT, writeValue(results.getResults(), serializationView), results.getResultSize(), results.hasLimitedResultSetSize(), writeValue(results.getStudyDateRangeLimitResults(), serializationView)), MediaType.APPLICATION_JSON);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @SuppressWarnings("unused")
    protected void sendNotification(final PacsServiceResourceContext context, final String subject, final String template) throws Exception {
        final String adminEmail = _siteConfigPreferences.getAdminEmail();
        context.put("pacs", getPacs());
        context.put("adminEmail", adminEmail);
        TurbineUtils.GetFullServerPath(getHttpServletRequest());
        final String body = AdminUtils.populateVmTemplate(context, "/screens/dqr/email/" + template + ".vm");
        _mailService.sendHtmlMessage(adminEmail, getUser().getEmail(), "[" + TurbineUtils.GetSystemName() + "] " + subject, body);
    }

    protected Pacs getPacs() throws PacsNotFoundException, DataFormatException {
        return getPacsHelper(getRequest());
    }

    protected Pacs getPacs(final Request request) throws PacsNotFoundException, DataFormatException {
        return getPacsHelper(request);
    }

    private Pacs getPacsHelper(final Request request) throws DataFormatException, PacsNotFoundException {
        final long pacsId = getPacsId(request);
        final Pacs pacs   = _pacsEntityService.retrieve(pacsId);
        if (null == pacs) {
            throw new PacsNotFoundException(pacsId);
        }
        return pacs;
    }

    protected static Long getPacsId(final Request request) throws DataFormatException {
        final String pacsId = getParameter(request, "PACS_ID").toString();
        try {
            return Long.valueOf(pacsId);
        } catch (NumberFormatException e) {
            throw new DataFormatException(pacsId);
        }
    }

    private static final String SEARCH_RESULTS_FORMAT = "{\"ResultSet\": {\"Result\": %s, \"resultSetSize\": \"%s\", \"limitedResultSetSize\": %b, \"studyDateRangeLimitResults\": %s}}";
    private static final String DOMAIN_OBJECT_FORMAT  = "{\"ResultSet\":{\"Result\": %s, \"limited\": \"false\"}}";

    private final PacsEntityService                 _pacsEntityService;
    private final PacsService                       _pacsService;
    private final StudyRoutingService               _studyRoutingService;
    private final DqrPreferences                    _dqrPreferences;
    private final DqrAdminSettingsForProjectService _dqrAdminSettings;
    private final QueuedPacsRequestService          _queuedPacsRequestService;
    private final SiteConfigPreferences             _siteConfigPreferences;
    private final MailService                       _mailService;
}
