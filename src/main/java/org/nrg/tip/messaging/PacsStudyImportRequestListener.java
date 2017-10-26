/*
 * org.nrg.tip.messaging.PacsStudyImportRequestListener
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.messaging;

import org.nrg.tip.services.PacsService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.XFT;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@SuppressWarnings("unused")
public class PacsStudyImportRequestListener {

    private final static Logger log = LoggerFactory.getLogger(PacsStudyImportRequestListener.class);

    public void onPacsStudyImportRequest(final PacsStudyImportRequest pacsStudyImportRequest) throws Exception {
        try {
            final PacsService pacsService = XDAT.getContextService().getBean(PacsService.class);

            log.info("Listener received study import request");
            for (final PacsSeriesImportRequest pacsSeriesImportRequest : pacsStudyImportRequest.getSeries()) {
                pacsService.importSeries(pacsStudyImportRequest.getRequestingUser(), pacsStudyImportRequest.getPacs(),
                        pacsSeriesImportRequest.getStudy(), pacsSeriesImportRequest.getSeries());
            }
            sendCompleteNotification(pacsStudyImportRequest);
            log.info("Listener completed study import request");
        } catch (final Exception e) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request " + pacsStudyImportRequest + " with the following error:\n" + e);
            throw e;
        }
    }

    private void sendCompleteNotification(final PacsStudyImportRequest request) throws Exception {
        // refresh the user, just in case their email has changed since they made the request
        try {
            final XDATUser user = new XDATUser(request.getRequestingUser().getLogin());
            XDAT.getMailService().sendMessage(XDAT.getSiteConfigPreferences().getAdminEmail(), user.getEmail(),
                    "PACS Study Import Request Complete",
                    "The study you requested from the PACS has been successfully imported into TIP.");
            final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_COMPLETE");
            PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, request.getStudy().getStudyId(), request.getStudy().getProjectId(), eventDetails);
            assert wrk != null;
            PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
        } catch (final UserNotFoundException e) {
            // not much to do here - was their account deleted since they made the request?
            log.error(String.format("User %s queued up a PACS import request, but their user account cannot be found to send them a confirmation email.", request.getRequestingUser().getLogin()));
        }
    }
}
