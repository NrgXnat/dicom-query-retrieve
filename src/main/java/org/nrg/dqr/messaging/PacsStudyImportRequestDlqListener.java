/*
 * PacsStudyImportRequestDlqListener
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

import lombok.extern.slf4j.Slf4j;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.turbine.utils.TurbineUtils;

@Slf4j
public class PacsStudyImportRequestDlqListener {
    public void onPacsStudyImportRequest(final PacsStudyImportRequest pacsStudyImportRequest) throws Exception {
        try {
            log.info("DLQ listener received study import request");
            sendFailureNotification(pacsStudyImportRequest);
            log.info("DLQ listener completed study import request");
        } catch (final Exception e) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request " + pacsStudyImportRequest + " with the following error:\n" + e);
            throw e;
        }
    }

    private void sendFailureNotification(final PacsStudyImportRequest pacsStudyImportRequest) throws Exception {
        // refresh the user, just in case their email has changed since they made the request
        try {
            final XDATUser user = new XDATUser(pacsStudyImportRequest.getRequestingUser().getLogin());
            XDAT.getMailService().sendMessage(XDAT.getSiteConfigPreferences().getAdminEmail(), new String[] { user.getEmail() }, new String[] { XDAT.getSiteConfigPreferences().getAdminEmail() },
                    "[" + TurbineUtils.GetSystemName()+"] PACS Study Import Request FAILED", "Sorry!  The system was unable to import the study you requested from the PACS.  We're looking into it...");
        } catch (final UserNotFoundException e) {
            // not much to do here - was their account deleted since they made the request?
            log.error(String.format("User %s queued up a PACS import request, but their user account cannot be found to send them a failure email.",
                    pacsStudyImportRequest.getRequestingUser().getLogin()));
        }
    }
}
