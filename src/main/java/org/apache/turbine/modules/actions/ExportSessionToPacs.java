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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xdat.om.XnatExperimentdata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;
import org.nrg.xnat.restlet.extensions.PacsNotStorableException;

import java.util.Arrays;
import java.util.concurrent.atomic.AtomicInteger;

@Slf4j
public class ExportSessionToPacs extends DqrSecureAction {
    public ExportSessionToPacs() {
    }

    @Override
    public void doPerform(final RunData data, final Context context) throws PacsNotFoundException, InsufficientPrivilegesException, DataFormatException {
        final UserI  user      = getUser();
        final String project   = (String) TurbineUtils.GetPassedParameter("project", data);
        final Pacs   pacs      = getPassedPacs(data);
        final String sessionId = (String) TurbineUtils.GetPassedParameter("session", data);
        if (StringUtils.isBlank(sessionId)) {
            throw new RuntimeException("You must specify a session ID for this operation.");
        }

        final XnatExperimentdata temp = XnatExperimentdata.getXnatExperimentdatasById(sessionId, user, false);
        if (!(temp instanceof XnatImagesessiondata)) {
            throw new RuntimeException("Couldn't find a session corresponding to the submitted session ID: " + sessionId);
        }

        final XnatImagesessiondata session = (XnatImagesessiondata) temp;
        try {
            if (!Permissions.canRead(user, session)) {
                throw new InsufficientPrivilegesException("You do not have access to this session.");
            }
        } catch (Exception e) {
            throw new RuntimeException("Error checking permissions for session.");
        }
        if (!Roles.checkRole(user, "Dqr") && !Roles.checkRole(user, "Administrator") && !getDqrPreferences().getAllowAllUsersToUseDqr()) {
            throw new InsufficientPrivilegesException("You do not have access to DQR functionality.");
        }
        if (!getDqrPreferences().getAllowAllProjectsToUseDqr() && !getDqrAdminSettings().isDqrEnabledForProject(project)) {
            //You cannot import into a project that does not have DQR enabled.
            throw new RuntimeException("You cannot import into a project that does not have DQR enabled.");
        }
        final String[] scanIds = (String[]) TurbineUtils.GetPassedObjects("scansToExport", data);
        if (scanIds == null) {
            log.debug("No scan IDs found to export, returning.");
            context.put("numberOfProcessedScans", 0);
            context.put("sessionId", session.getId());
        } else {
            try {
                final int                 exported = exportOnDemand(user, pacs, session, scanIds);
                final PersistentWorkflowI workflow = PersistentWorkflowUtils.buildOpenWorkflow(getUser(), XnatMrsessiondata.SCHEMA_ELEMENT_NAME, session.getId(), project, EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "EXPORT_TO_PACS_REQUEST", null, String.format(FORMAT, exported, sessionId, pacs.getId(), pacs.getHost(), pacs.getQueryRetrievePort(), pacs.getAeTitle())));
                assert workflow != null;
                PersistentWorkflowUtils.complete(workflow, workflow.buildEvent());

                context.put("numberOfProcessedScans", scanIds.length);
                context.put("sessionId", session.getId());
                context.put("_user", user);
                //noinspection InstantiationOfUtilityClass
                context.put("StringUtils", new StringUtils());

                log.debug("User {} exported {} scans from session {}", user.getUsername(), scanIds.length, session.getId());
                getMailService().sendMessage(getSiteConfigPreferences().getAdminEmail(), user.getEmail(),
                                             "[" + TurbineUtils.GetSystemName() + "] PACS Session Export Request Complete",
                                             "The session you requested has been successfully exported to the PACS.");
            } catch (Exception exception) {
                context.put("sessionId", session.getId());
                context.put("scanIds", StringUtils.join(scanIds, ", "));
                context.put("host", pacs.getHost());
                context.put("aeTitle", pacs.getAeTitle());
                context.put("qrPort", pacs.getQueryRetrievePort() == null ? "N/A" : pacs.getQueryRetrievePort().toString());
                context.put("user", user);
                //noinspection InstantiationOfUtilityClass
                context.put("StringUtils", new StringUtils());
                context.put("error", exception);
                data.setScreenTemplate("ExportSessionToPacsError.vm");
            }
        }

        data.setScreenTemplate("ExportSessionToPacsRequested.vm");
    }

    private int exportOnDemand(final UserI user, final Pacs pacs, final XnatImagesessiondata session, final String[] scanIds) throws PacsNotStorableException {
        if (!pacs.isStorable()) {
            throw new PacsNotStorableException(pacs.getId());
        }
        final AtomicInteger count = new AtomicInteger();
        Arrays.stream(scanIds).map(session::getScanById).forEach(scan -> {
            getPacsService().exportSeries(user, pacs, scan);
            log.info("Exported series {} from session {}", scan.getId(), session.getId());
            count.incrementAndGet();
        });
        return count.get();
    }

    private static final String FORMAT = "Exported %d scans from session %s to PACS %d: %s:%d AE %s";
}
