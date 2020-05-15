/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsStudyImportRequestListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.extern.slf4j.Slf4j;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.services.PacsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PacsStudyImportRequestListener extends AbstractPacsRequestListener<PacsStudyImportRequest> {
    @Autowired
    public PacsStudyImportRequestListener(final PacsService pacsService, final SiteConfigPreferences siteConfigPreferences, final MailService mailService) {
        super(pacsService, siteConfigPreferences, mailService);
    }

    @JmsListener(id = "pacsStudyImportRequest", destination = "pacsStudyImportRequest")
    public void onRequest(final PacsStudyImportRequest request) throws Exception {
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
            log.error("Choked on request {} with the following error", request, e);
            throw e;
        }
    }
}
