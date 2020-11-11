/*
 * org.nrg.xnat.restlet.extensions.PacsSeriesImporter
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.apache.http.HttpHeaders;
import org.nrg.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xdat.security.helpers.Groups;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.resource.Representation;
import org.restlet.resource.Variant;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

// NOTE: Removed this URL in favor of requiring all data in body "/services/pacs/{PACS_ID}/import/study/{STUDY_ID}/series/{SERIES_ID}"
@Deprecated
@XnatRestlet("/services/pacs/{PACS_ID}/import/series")
@Slf4j
public class PacsSeriesImporter extends PacsServiceResource {
    public PacsSeriesImporter(final Context context, final Request request, final Response response) {
        super(context, request, response);
        _studyInstanceUid = getBodyVariable("STUDY_ID");
        if (StringUtils.isBlank(_studyInstanceUid)) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, new RuntimeException("For the best level of compatibility across PACS, you should always specify the study instance UID for the DICOM study that contains the desired DICOM series."), "For the best level of compatibility across PACS, you should always specify the study instance UID for the DICOM study that contains the desired DICOM series.");
        }
        final String seriesId = (String) getParameter(getRequest(), "SERIES_ID");
        final List<String> seriesIds1 = new ArrayList<>();
        if (!StringUtils.isBlank(seriesId)) {
            seriesIds1.add(seriesId);
        }
        final String seriesIds = getBodyVariable("SERIES_IDS");
        if (!StringUtils.isBlank(seriesIds)) {
            seriesIds1.addAll(Arrays.asList(seriesIds.split("\\s*,\\s*")));
        }
        _ae = getBodyVariable("AE");
        _projectId = getBodyVariable("PROJECT");
        if (StringUtils.isBlank(_projectId)) {
            log.debug("No project ID set for study instance UID: {}, series {}", _studyInstanceUid, String.join(", ", seriesIds1));
        } else {
            log.debug("The project {} will be set as the destination for study instance UID: {}, series {}", _projectId, _studyInstanceUid, String.join(", ", seriesIds1));
        }
    }

    @Override
    public void handlePut() {
        UserI user = getUser();
        if (user.isGuest()) {
            respondWithNeedToBeLoggedIn();
        } else if (!Roles.checkRole(user, "Dqr") && !Roles.checkRole(user, "Administrator") && !getDqrPreferences().getAllowAllUsersToUseDqr()) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You do not have access to DQR functionality.");
        } else if (!Permissions.canEditProject(user, _projectId) && !Roles.checkRole(user, "Administrator") && !Groups.hasAllDataAccess(user)) {
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Your user does not have permission to import.");
        } else {
            if (!getDqrPreferences().getAllowAllProjectsToUseDqr() && !getDqrAdminSettings().isDqrEnabledForProject(_projectId)) {
                //You cannot import into a project that does not have DQR enabled.
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You cannot import into a project that does not have DQR enabled.");
            } else {
                String destinationAeTitle = _ae;
                if (_ae != null && _ae.contains(":")) {
                    String[] parts = _ae.split(":");
                    destinationAeTitle = parts[0];
                }
                try {
                    final QueuedPacsRequest pacsReq = new QueuedPacsRequest();
                    pacsReq.setPacsId(getPacsId(getRequest()));
                    pacsReq.setUsername(getUser().getUsername());
                    pacsReq.setXnatProject(_projectId);
                    pacsReq.setStudyInstanceUid(_studyInstanceUid);
                    pacsReq.setSeriesIds(getBodyVariable("SERIES_IDS"));
                    pacsReq.setDestinationAeTitle(destinationAeTitle);
                    pacsReq.setPriority(PacsRequest.HIGH_PRIORITY);
                    pacsReq.setStatus(PacsRequest.QUEUED_STATUS_TEXT);
                    pacsReq.setQueuedTime(new Date());

                    getQueuedPacsRequestService().create(pacsReq);
                    getResponse().setStatus(Status.SUCCESS_OK);

                    final Form responseHeaders = new Form();
                    getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
                    responseHeaders.add(HttpHeaders.WARNING, "Your request is queued and will be serviced when the PACS is available.");
                } catch (Exception e) {
                    final Throwable cause = e.getCause();
                    if (!(cause instanceof Exception)) {
                        respondToException(e, Status.SERVER_ERROR_INTERNAL);
                    } else if (cause instanceof CMoveFailureException) {
                        final CMoveFailureException failure = (CMoveFailureException) cause;
                        log.error("C-MOVE operation failed:\n" + failure.getMessage(), failure);
                        getResponse().setStatus(Status.SERVER_ERROR_INTERNAL, failure.getMessage());
                    } else if (cause instanceof CMoveTargetNotFoundException) {
                        respondToException((CMoveTargetNotFoundException) cause, Status.SERVER_ERROR_INTERNAL);
                    } else {
                        respondToException((Exception) cause, Status.SERVER_ERROR_INTERNAL);
                    }
                }
            }
        }
    }

    @Override
    public boolean allowGet() {
        return false;
    }

    @Override
    public boolean allowPut() {
        return true;
    }

    @Override
    protected Representation representImpl(final Variant variant) {
        log.debug("This should never be called.");
        return null;
    }

    private final String       _projectId;
    private final String       _studyInstanceUid;
    private final String       _ae;
}
