/*
 * PacsSessionExportRequestListener
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.messaging;

import com.google.common.base.Joiner;
import org.nrg.dqr.services.PacsService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

@SuppressWarnings("unused")
public class PacsSessionExportRequestListener {

    private final static Logger log = LoggerFactory.getLogger(PacsSessionExportRequestListener.class);

    public void onPacsSessionExportRequest(final PacsSessionExportRequest pacsSessionExportRequest) throws Exception {
        try {
            final PacsService pacsService = XDAT.getContextService().getBean(PacsService.class);

            log.info("Listener received session export request");
            for (final PacsScanExportRequest pacsScanExportRequest : pacsSessionExportRequest.getScans()) {
                pacsService.exportSeries(pacsSessionExportRequest.getRequestingUser(),
                        pacsSessionExportRequest.getPacs(), pacsScanExportRequest.getScan());
            }
            sendCompleteNotification(pacsSessionExportRequest);
            log.info("Listener completed session export request");
        } catch (final Exception e) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request " + pacsSessionExportRequest + " with the following error:\n" + e);
            throw e;
        }
    }

    private void sendCompleteNotification(final PacsSessionExportRequest pacsSessionExportRequest) throws Exception {
        // refresh the user, just in case their email has changed since they made the request
        try {
            final XDATUser user = new XDATUser(pacsSessionExportRequest.getRequestingUser().getLogin());
            XDAT.getMailService().sendMessage(XDAT.getSiteConfigPreferences().getAdminEmail(), user.getEmail(),
                    "PACS Session Export Request Complete",
                    "The session you requested has been successfully exported to the PACS.");
            final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "EXPORT_TO_PACS_COMPLETE");
            eventDetails.setComment("Series: " + Joiner.on(", ").join(getSeriesIds(pacsSessionExportRequest.getScans())));
            PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, pacsSessionExportRequest.getSession().getId(), pacsSessionExportRequest.getSession().getProject(), eventDetails);
            assert wrk != null;
            PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
        } catch (final UserNotFoundException e) {
            // not much to do here - was their account deleted since they made the request?
            log.error(String.format("User %s queued up a PACS export request, but their user account cannot be found to send them a confirmation email.",
                    pacsSessionExportRequest.getRequestingUser().getLogin()));
        }
    }

    private List<String> getSeriesIds(final List<PacsScanExportRequest> scans) {
        final List<String> seriesIds = new ArrayList<>();
        for (final PacsScanExportRequest request : scans) {
            seriesIds.add(request.getScan().getId());
        }
        return seriesIds;
    }
}
