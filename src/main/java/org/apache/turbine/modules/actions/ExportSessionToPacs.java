/*
 * ExportSessionToPacs
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.modules.actions;

import com.google.common.base.Joiner;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatExperimentdata;
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
        _user = XDAT.getUserDetails();

        getPassedPacs(data);

        final String project = (String) TurbineUtils.GetPassedParameter("project", data);
        if (!getDqrPreferences().getAllowAllProjectsToUseDqr() && !getDqrAdminSettings().isDqrEnabledForProject(project)) {
            //You cannot import into a project that does not have DQR enabled.
            throw new RuntimeException("You cannot import into a project that does not have DQR enabled.");
        }

        final String session = (String) TurbineUtils.GetPassedParameter("session", data);
        if (StringUtils.isBlank(session)) {
            throw new RuntimeException("You must specify a session ID for this operation.");
        }

        final XnatExperimentdata experiment = XnatExperimentdata.getXnatExperimentdatasById(session, _user, false);
        if (experiment instanceof XnatImagesessiondata) {
            _session = (XnatImagesessiondata) experiment;
        }
        if (_session == null) {
            throw new RuntimeException("Couldn't find a session corresponding to the submitted session ID: " + session);
        }
        try {
            if (!Permissions.canRead(_user, _session)) {
                throw new RuntimeException("You do not have access to this session.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error checking permissions for session.");
        }
        if (!Roles.checkRole(_user, "Dqr") && !Roles.checkRole(_user, "Administrator") && !getDqrPreferences().getAllowAllUsersToUseDqr()) {
            throw new RuntimeException("You do not have access to DQR functionality.");
        }
        try {
            _scanIds = (String[]) TurbineUtils.GetPassedObjects("scansToExport", data);
            if (_scanIds == null) {
                log.debug("No scan IDs found to export, returning.");
                context.put("numberOfProcessedScans", 0);
                context.put("sessionId", _session.getId());
            } else {
                exportOnDemand();
                final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "EXPORT_TO_PACS_REQUEST");
                eventDetails.setComment("Pacs: " + getPacs().getId());
                final PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(getUser(), XnatMrsessiondata.SCHEMA_ELEMENT_NAME, _session.getId(), project, eventDetails);
                assert wrk != null;
                PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
                context.put("numberOfProcessedScans", _scanIds.length);
                context.put("sessionId", _session.getId());
                context.put("_user", _user);
                log.debug("User {} exported {} scans from session {}", _user.getLogin(), _scanIds.length, _session.getId());
                getMailService().sendMessage(getSiteConfigPreferences().getAdminEmail(), _user.getEmail(),
                                             "[" + TurbineUtils.GetSystemName() + "] PACS Session Export Request Complete",
                                             "The session you requested has been successfully exported to the PACS.");
            }

            data.setScreenTemplate("ExportSessionToPacsRequested.vm");
        } catch (Exception exception) {
            context.put("sessionId", _session.getId());
            context.put("scanIds", Joiner.on(", ").join(_scanIds));
            context.put("host", getPacs().getHost());
            context.put("aeTitle", getPacs().getAeTitle());
            try {
                final Integer queryRetrievePort = getPacs().getQueryRetrievePort();
                context.put("qrPort", queryRetrievePort == null ? "N/A" : queryRetrievePort.toString());
            } catch (Exception e1) {
                context.put("qrPort", "N/A");
            }
            context.put("user", _user);
            context.put("error", exception);
            data.setScreenTemplate("ExportSessionToPacsError.vm");
        }
    }

    private void exportOnDemand() throws PacsNotStorableException {
        if (!getPacs().isStorable()) {
            throw new PacsNotStorableException(getPacsId());
        }
        for (final String scanId : _scanIds) {
            getPacsService().exportSeries(_user, getPacs(), _session.getScanById(scanId));
            log.info("Exported series {} from session {}", scanId, _session.getId());
        }
    }

    private XnatImagesessiondata _session;
    private String[]             _scanIds;
    private UserI                _user;
}
