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

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import org.apache.commons.lang.StringUtils;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.DqrDomainObject;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.restlet.NullValueSerializer;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.dqr.services.PacsService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.restlet.resources.SecureResource;
import org.restlet.Context;
import org.restlet.data.MediaType;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.StringRepresentation;
import org.restlet.resource.Variant;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

public abstract class PacsServiceResource extends SecureResource {

    public PacsServiceResource(final Context context, final Request request, final Response response) {
        super(context, request, response);
        this.getVariants().add(new Variant(MediaType.ALL));
        pacsService = initPacsService();
        spaService = XDAT.getContextService().getBean(StudyRoutingService.class);
    }

    public PacsService getPacsService() {
        return pacsService;
    }

    public void respondWithNotFound() {
        respondWithNotFound("Nothing found that matches this request");
    }

    public void respondWithPacsNotFound() {
        respondWithNotFound("No PACS were found that match this request");
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
            Representation r = new StringRepresentation("{\"ResultSet\":{\"Result\":"
                    + getObjectMapper().writerWithView(serializationView).writeValueAsString(result) + ", \"limited\": \""
                    + false + "\"}}");
            r.setMediaType(MediaType.APPLICATION_JSON);
            return r;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected Study assignStudyToProject(final String projectId, final String studyInstanceUid) {
        if (!StringUtils.isBlank(projectId)) {
            if (_log.isDebugEnabled()) {
                _log.debug("Assigning study instance UID " + studyInstanceUid + " to project " + projectId);
            }
            spaService.assign(studyInstanceUid, projectId, getUser().getLogin());
            return new Study(projectId, studyInstanceUid);
        } else {
            if (_log.isDebugEnabled()) {
                _log.debug("No project assignment specified for study instance UID " + studyInstanceUid + ", may be registered as Unassigned");
            }
            return new Study(studyInstanceUid);
        }
    }

    protected Representation jsonRepresentation(final PacsSearchResults<?, ?> results, final Class<?> serializationView) {
        try {
            Representation r = new StringRepresentation("{\"ResultSet\":{\"Result\":"
                    + getObjectMapper().writerWithView(serializationView).writeValueAsString(results.getResults())
                    + ", \"resultSetSize\":\""
                    + results.getResultSize()
                    + "\""
                    + ", \"limitedResultSetSize\":"
                    + results.hasLimitedResultSetSize()
                    + ", \"studyDateRangeLimitResults\":"
                    + getObjectMapper().writerWithView(serializationView).writeValueAsString(
                    results.getStudyDateRangeLimitResults()) + "}}");
            r.setMediaType(MediaType.APPLICATION_JSON);
            return r;
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    protected void sendNotification(final PacsServiceResourceContext context, final String subject, final String template) throws Exception {
        final String adminEmail = XDAT.getSiteConfigPreferences().getAdminEmail();
        context.put("pacs", getPacs());
        context.put("adminEmail", adminEmail);
        TurbineUtils.GetFullServerPath(getHttpServletRequest());
        final String body = AdminUtils.populateVmTemplate(context, "/screens/dqr/email/" + template + ".vm");
        XDAT.getMailService().sendHtmlMessage(adminEmail, getUser().getEmail(), "[" + TurbineUtils.GetSystemName()+"] " + subject, body);
    }

    protected Pacs getPacs() throws PacsNotFoundException {
        return getPacsHelper(getRequest());
    }

    protected ObjectMapper getObjectMapper() {
        return MAPPER;
    }

    static PacsService initPacsService() {
        return XDAT.getContextService().getBean(PacsService.class);
    }

    static Pacs getPacs(final Request request) throws PacsNotFoundException {
        return getPacsHelper(request);
    }

    private static Pacs getPacsHelper(final Request request) throws PacsNotFoundException {
        PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        Pacs pacs = pacsEntityService.retrieve(getPacsId(request));
        if (null == pacs) {
            throw new PacsNotFoundException();
        } else {
            return pacs;
        }
    }

    protected static Long getPacsId(final Request request) throws PacsNotFoundException {
        try {
            return Long.valueOf(getParameter(request, "PACS_ID").toString());
        } catch (NumberFormatException e) {
            throw new PacsNotFoundException();
        }
    }

    private final static Logger _log = LoggerFactory.getLogger(PacsServiceResource.class);

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        final DefaultSerializerProvider provider = new DefaultSerializerProvider.Impl();
        provider.setNullValueSerializer(new NullValueSerializer());
        MAPPER.setSerializerProvider(provider);
    }

    private final PacsService pacsService;
    private final StudyRoutingService spaService;
}
