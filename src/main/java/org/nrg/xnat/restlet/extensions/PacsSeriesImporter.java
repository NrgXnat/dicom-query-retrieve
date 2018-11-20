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

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.apache.http.Header;
import org.apache.http.HttpHeaders;
import org.apache.http.client.methods.HttpHead;
import org.nrg.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.dqr.domain.entities.DqrAdminSettingsForProject;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.services.DqrAdminSettingsForProjectService;
import org.nrg.dqr.services.ExecutedPacsRequestService;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.dqr.services.QueuedPacsRequestService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.helpers.Groups;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.merge.anonymize.DefaultAnonUtils;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.Form;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.restlet.util.Series;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.LocalTime;
import java.util.*;

// NOTE: Removed this URL in favor of requiring all data in body "/services/pacs/{PACS_ID}/import/study/{STUDY_ID}/series/{SERIES_ID}"
@XnatRestlet("/services/pacs/{PACS_ID}/import/series")
public class PacsSeriesImporter extends PacsServiceResource {

    public PacsSeriesImporter(final Context context, final Request request, final Response response) {
        super(context, request, response);
        _studyInstanceUid = getBodyVariable("STUDY_ID");
        if (StringUtils.isBlank(_studyInstanceUid)) {
            response.setStatus(Status.CLIENT_ERROR_BAD_REQUEST, new RuntimeException("For the best level of compatibility across PACS, you should always specify the study instance UID for the DICOM study that contains the desired DICOM series."), "For the best level of compatibility across PACS, you should always specify the study instance UID for the DICOM study that contains the desired DICOM series.");
        }
        final String seriesId = (String) getParameter(getRequest(), "SERIES_ID");
        if (!StringUtils.isBlank(seriesId)) {
            _seriesIds.add(seriesId);
        }
        final String seriesIds = getBodyVariable("SERIES_IDS");
        if (!StringUtils.isBlank(seriesIds)) {
            _seriesIds.addAll(Arrays.asList(seriesIds.split("\\s*,\\s*")));
        }
        _ae = getBodyVariable("AE");
        _projectId = getBodyVariable("PROJECT");
        if (StringUtils.isBlank(_projectId)) {
            if (_log.isDebugEnabled()) {
                _log.debug("No project ID set for study instance UID: " + _studyInstanceUid + ", series " + Joiner.on(", ").join(_seriesIds));
            }
        } else if (_log.isDebugEnabled()) {
            _log.debug("The project " + _projectId + " will be set as the destination for study instance UID: " + _studyInstanceUid + ", series " + Joiner.on(", ").join(_seriesIds));
        }
    }

    @Override
    public void handlePut() {
        UserI user = getUser();
        if(user.isGuest()){
            respondWithNeedToBeLoggedIn();
        }
        else if(!Roles.checkRole(user,"Dqr") && !Roles.checkRole(user,"Administrator")){
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Your user must have the Dqr role to import using DQR.");
        }
        else if(!Permissions.canEditProject(user, _projectId) && !Roles.checkRole(user,"Administrator") && !Groups.hasAllDataAccess(user)){
            getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "Your user does not have permission to import.");
        }
        else {
            DqrAdminSettingsForProject existingSettings = XDAT.getContextService().getBean(DqrAdminSettingsForProjectService.class).findSettingsByProject(_projectId);
            if (existingSettings == null) {
                //You cannot import into a project that does not have DQR enabled.
                getResponse().setStatus(Status.CLIENT_ERROR_FORBIDDEN, "You cannot import into a project that does not have DQR enabled.");
            } else {
                String destinationAeTitle = _ae;
                if (_ae != null && _ae.contains(":")) {
                    String[] parts = _ae.split(":");
                    destinationAeTitle = parts[0];
                }
                try {
                    final Pacs pacs = getPacs();
                    PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
                    boolean pacsIsAvailable = pacsEntityService.isAvailable(pacs);

                    if (pacsIsAvailable) {
                        try {
                            String script = DefaultAnonUtils.getService().getStudyScript(_studyInstanceUid);
                            if (StringUtils.isNotBlank(script)) {
                                DefaultAnonUtils.getService().disableStudy(AdminUtils.getAdminUser().getLogin(), _studyInstanceUid);
                            }
                        } catch (Exception e) {
                            _log.error("Error when clearing study remapping information.", e);
                        }

                        ExecutedPacsRequest pacsReq = new ExecutedPacsRequest();
                        pacsReq.setPacsId(getPacsId(getRequest()));
                        pacsReq.setUsername(getUser().getUsername());
                        pacsReq.setXnatProject(_projectId);
                        pacsReq.setStudyInstanceUid(_studyInstanceUid);
                        pacsReq.setSeriesIds(getBodyVariable("SERIES_IDS"));
                        pacsReq.setDestinationAeTitle(destinationAeTitle);
                        pacsReq.setExecutedTime(new Date());

                        XDAT.getContextService().getBean(ExecutedPacsRequestService.class).create(pacsReq);

                        getPacsService().importFromPacsRequest(pacsReq);

                        final String siteUrl = XDAT.getSiteConfigPreferences().getSiteUrl();
                        final StringBuilder prearchive = new StringBuilder(siteUrl);
                        if (!siteUrl.endsWith("/")) {
                            prearchive.append("/");
                        }
                        prearchive.append("app/template/XDATScreen_prearchives.vm");

                        final PacsServiceResourceContext context = new PacsServiceResourceContext();
                        context.put("prearchive", prearchive.toString());
                        context.put("studyId", _studyInstanceUid);
                        context.put("seriesIds", _seriesIds);

                        try {
                            if (_log.isDebugEnabled()) {
                                _log.debug("Completed DICOM request for study " + _studyInstanceUid + (StringUtils.isBlank(_projectId) ? " with no project assignment." : " assigned to project " + _projectId));
                            }
                            sendNotification(context, "Selected DICOM series requested", "SeriesRequested");
                        } catch (Exception exception) {
                            _log.warn("User " + getUser().getLogin() + " successfully requested one or more DICOM series, but an error occurred sending the notification email.", exception);
                        }

                        final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST");
                        eventDetails.setComment("Series: " + Joiner.on(", ").join(_seriesIds));
                        PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(getUser(), XnatMrsessiondata.SCHEMA_ELEMENT_NAME, _studyInstanceUid, _projectId, eventDetails);
                        assert wrk != null;
                        PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
                    } else {
                        QueuedPacsRequest pacsReq = new QueuedPacsRequest();
                        pacsReq.setPacsId(getPacsId(getRequest()));
                        pacsReq.setUsername(getUser().getUsername());
                        pacsReq.setXnatProject(_projectId);
                        pacsReq.setStudyInstanceUid(_studyInstanceUid);
                        pacsReq.setSeriesIds(getBodyVariable("SERIES_IDS"));
                        pacsReq.setDestinationAeTitle(destinationAeTitle);
                        pacsReq.setQueuedTime(new Date());

                        XDAT.getContextService().getBean(QueuedPacsRequestService.class).create(pacsReq);
                        getResponse().setStatus(Status.SUCCESS_OK);

                        Form responseHeaders = new Form();
                        getResponse().getAttributes().put("org.restlet.http.headers", responseHeaders);
                        responseHeaders.add(HttpHeaders.WARNING, "This PACS is not currently available, but your request is queued and will be serviced when the PACS is available.");
                    }
                } catch (final PacsNotFoundException exception) {
                    _log.warn("PACS not found somehow", exception);
                    respondWithPacsNotFound();
                } catch (final PacsNotQueryableException exception) {
                    _log.warn("PACS not queryable somehow", exception);
                    respondWithPacsNotFound();
                } catch (final PacsNotStorableException exception) {
                    _log.warn("PACS not storable somehow", exception);
                    respondWithPacsNotFound();
                } catch (final PacsNotAvailableException exception) {
                    _log.warn("PACS not available at this time", exception);
                    respondWithPacsNotFound();
                } catch (PersistentWorkflowUtils.ActionNameAbsent e) {
                    _log.warn("Error creating new workflow event", e);
                    respondToException(e, Status.SERVER_ERROR_INTERNAL);
                } catch (PersistentWorkflowUtils.IDAbsent e) {
                    _log.warn("ID absent when creating new workflow event", e);
                    respondToException(e, Status.SERVER_ERROR_INTERNAL);
                } catch (PersistentWorkflowUtils.JustificationAbsent e) {
                    _log.warn("Justification absent but required when creating new workflow event", e);
                    respondToException(e, Status.SERVER_ERROR_INTERNAL);
                } catch (Exception e) {
                    final Throwable cause = e.getCause();
                    if (cause == null || !(cause instanceof Exception)) {
                        respondToException(e, Status.SERVER_ERROR_INTERNAL);
                    } else if (cause instanceof CMoveFailureException) {
                        final CMoveFailureException failure = (CMoveFailureException) cause;
                        _log.error("C-MOVE operation failed:\n" + failure.getMessage(), failure);
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

    private static final Logger _log = LoggerFactory.getLogger(PacsSeriesImporter.class);

    private final String _projectId;
    private final String _studyInstanceUid;
    private final String _ae;
    private final List<String> _seriesIds = new ArrayList<>();
}
