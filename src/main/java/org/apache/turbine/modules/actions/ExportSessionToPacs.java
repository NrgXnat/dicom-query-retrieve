/*
 * dicom-query-retrieve: org.apache.turbine.modules.actions.ExportSessionToPacs
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.actions;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;

@SuppressWarnings("unused")
@Slf4j
public class ExportSessionToPacs extends DqrSecureAction {
    @Override
    public void doPerform(final RunData data, final Context context) throws PacsNotFoundException, NotFoundException {
        final UserI user = XDAT.getUserDetails();

        getPassedPacs(data);

        final String project = (String) TurbineUtils.GetPassedParameter("project", data);
        if (!getDqrPreferences().getAllowAllProjectsToUseDqr() && !getDqrAdminSettings().isDqrEnabledForProject(project)) {
            //You cannot import into a project that does not have DQR enabled.
            throw new RuntimeException("You cannot import into a project that does not have DQR enabled.");
        }

        final String sessionId = (String) TurbineUtils.GetPassedParameter("session", data);
        if (StringUtils.isBlank(sessionId)) {
            throw new RuntimeException("You must specify a session ID for this operation.");
        }

        final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(sessionId, user, false);
        if (session == null) {
            throw new RuntimeException("Couldn't find a session corresponding to the submitted session ID: " + sessionId);
        }
        try {
            if (!Permissions.canRead(user, session)) {
                throw new RuntimeException("You do not have access to this session.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error checking permissions for session.");
        }
        if (!Roles.checkRole(user, "Dqr") && !Roles.checkRole(user, "Administrator") && !getDqrPreferences().getAllowAllUsersToUseDqr()) {
            throw new RuntimeException("You do not have access to DQR functionality.");
        }
        final String[] scanIds = (String[]) TurbineUtils.GetPassedObjects("scansToExport", data);
        try {
            data.setScreenTemplate("ExportSessionToPacsRequested.vm");
            if (scanIds == null || scanIds.length == 0) {
                log.debug("No scan IDs found to export, returning.");
                context.put("numberOfProcessedScans", 0);
                context.put("sessionId", session.getId());
                return;
            }                if (!getPacs().isStorable()) {
                throw new PacsNotStorableException(getPacsId());
            }
            for (final String scanId : scanIds) {
                getPacsService().exportSeries(user, getPacs(), session.getScanById(scanId));
                log.info("Exported series {} from session {}", scanId, session.getId());
            }

            // TODO: When queued operations are supported properly, use this instead of the synchronous code below.
            // XDAT.sendJmsRequest(PacsSessionExportRequest.builder().username(user.getUsername()).pacs(pacs).sessionId(sessionId).scans(Arrays.stream(scanIds).map(PacsScanExportRequest::new).collect(Collectors.toList())).build());

            final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "EXPORT_TO_PACS_REQUEST");
            eventDetails.setComment("Pacs: " + getPacs().getId());
            final PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(getUser(), XnatMrsessiondata.SCHEMA_ELEMENT_NAME, session.getId(), project, eventDetails);
            assert wrk != null;
            PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
            context.put("numberOfProcessedScans", scanIds.length);
            context.put("sessionId", session.getId());
            context.put("_user", user);
            log.debug("User {} exported {} scans from session {}", user.getLogin(), scanIds.length, session.getId());
            getMailService().sendMessage(getSiteConfigPreferences().getAdminEmail(), user.getEmail(),
                                         "[" + TurbineUtils.GetSystemName() + "] PACS Session Export Request Complete",
                                         "The session you requested has been successfully exported to the PACS.");
        } catch (Exception exception) {
            context.put("sessionId", session.getId());
            context.put("scanIds", StringUtils.join(scanIds, ", "));
            context.put("host", getPacs().getHost());
            context.put("aeTitle", getPacs().getAeTitle());
            try {
                final Integer queryRetrievePort = getPacs().getQueryRetrievePort();
                context.put("qrPort", queryRetrievePort == null ? "N/A" : queryRetrievePort.toString());
            } catch (Exception e1) {
                context.put("qrPort", "N/A");
            }
            context.put("user", user);
            context.put("error", exception);
            data.setScreenTemplate("ExportSessionToPacsError.vm");
        }
    }
}
