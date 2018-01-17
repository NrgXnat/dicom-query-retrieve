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
import org.hsqldb.lib.StringUtil;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.dto.ApplicationEntity;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xnat.restlet.XnatRestlet;
import org.restlet.Context;
import org.restlet.data.Request;
import org.restlet.data.Response;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;

// NOTE: Removed this URL in favor of requiring all data in body "/services/pacs/{PACS_ID}/import/study/{STUDY_ID}/series/{SERIES_ID}"
@XnatRestlet("/services/pacs/{PACS_ID}/import/series")
public class PacsSeriesImporter extends PacsServiceResource {

    public PacsSeriesImporter(final Context context, final Request request, final Response response) {
        super(context, request, response);
        _studyId = getBodyVariable("STUDY_ID");
        if (StringUtils.isBlank(_studyId)) {
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
                _log.debug("No project ID set for study instance UID: " + _studyId + ", series " + Joiner.on(", ").join(_seriesIds));
            }
        } else if (_log.isDebugEnabled()) {
            _log.debug("The project " + _projectId + " will be set as the destination for study instance UID: " + _studyId + ", series " + Joiner.on(", ").join(_seriesIds));
        }
    }

    @Override
    public void handlePut() {
        try {
            final Pacs pacs = getPacs();
            if(!pacs.isQueryable()) {
                throw new PacsNotQueryableException();
            }
            else if(!getPacsService().aeIsStorable(_ae)){
                throw new PacsNotStorableException();
            }
            else {
                try {
                    final Study study = assignStudyToProject(_projectId, _studyId);

                    for (String seriesId : _seriesIds) {
                        if (_log.isDebugEnabled()) {
                            _log.debug("Requesting series " + seriesId + " for study instance UID " + _studyId);
                        }
                        getPacsService().importSeries(XDAT.getUserDetails(), pacs, study, new Series(seriesId), _ae);
                    }
                } catch (final CMoveTargetNotFoundException exception) {
                    _log.warn("C-MOVE target not found somehow: PACS [ aeTitle: " + pacs.getAeTitle() + ", ", exception);
                    respondWithNotFound("Unable to find the specified series.");
                }
            }

            final String siteUrl = XDAT.getSiteConfigPreferences().getSiteUrl();
            final StringBuilder prearchive = new StringBuilder(siteUrl);
            if (!siteUrl.endsWith("/")) {
                prearchive.append("/");
            }
            prearchive.append("app/template/XDATScreen_prearchives.vm");

            final PacsServiceResource.PacsServiceResourceContext context = new PacsServiceResource.PacsServiceResourceContext();
            context.put("prearchive", prearchive.toString());
            context.put("studyId", _studyId);
            context.put("seriesIds", _seriesIds);

            try {
                if (_log.isDebugEnabled()) {
                    _log.debug("Completed DICOM request for study " + _studyId + (StringUtils.isBlank(_projectId) ? " with no project assignment." : " assigned to project " + _projectId));
                }
                sendNotification(context, "Selected DICOM series requested", "SeriesRequested");
            } catch (Exception exception) {
                _log.warn("User " + getUser().getLogin() + " successfully requested one or more DICOM series, but an error occurred sending the notification email.", exception);
            }

            final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST");
            eventDetails.setComment("Series: " + Joiner.on(", ").join(_seriesIds));
            PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(getUser(), XnatMrsessiondata.SCHEMA_ELEMENT_NAME, _studyId, _projectId, eventDetails);
            assert wrk != null;
            PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
        } catch (final PacsNotFoundException exception) {
            _log.warn("PACS not found somehow", exception);
            respondWithPacsNotFound();
        } catch (final PacsNotQueryableException exception) {
            _log.warn("PACS not queryable somehow", exception);
            respondWithPacsNotFound();
        } catch (final PacsNotStorableException exception) {
            _log.warn("PACS not storable somehow", exception);
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
    private final String _studyId;
    private final String _ae;
    private final List<String> _seriesIds = new ArrayList<>();
}
