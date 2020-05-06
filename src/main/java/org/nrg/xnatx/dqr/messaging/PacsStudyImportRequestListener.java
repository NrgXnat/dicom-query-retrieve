/*
 * PacsStudyImportRequestListener
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.extern.slf4j.Slf4j;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;

@SuppressWarnings("unused")
@Slf4j
public class PacsStudyImportRequestListener extends AbstractPacsRequestListener {
    public void onPacsStudyImportRequest(final PacsStudyImportRequest request) throws Exception {
        try {
            final UserI user = Users.getUser(request.getRequestingUser());
            //Study import requests are not currently set up to allow users to specify which AE to send the data to
            log.info("Listener received study import request from user {}", user.getUsername());
            for (final PacsScanImportRequest scanImportRequest : request.getScans()) {
                getPacsService().importSeries(user, request.getPacs(), scanImportRequest.getStudy(), scanImportRequest.getSeries(), null);
            }
            getMailService().sendMessage(getAdminEmail(), user.getEmail(),
                                         "[" + TurbineUtils.GetSystemName() + "] PACS Study Import Request Complete",
                                         "The study you requested from the PACS has been successfully imported.");
            log.info("Listener completed study import request");
        } catch (final UserNotFoundException e) {
            // not much to do here - was their account deleted since they made the request?
            log.error("User {} queued up a PACS import request, but their user account cannot be found to send them a confirmation email.", request.getRequestingUser());
        } catch (final Exception e) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request " + request + " with the following error:\n" + e);
            throw e;
        }
    }
}
