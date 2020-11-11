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

package org.nrg.dqr.messaging;

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.services.PacsService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PacsStudyImportRequestListener {
    @Autowired
    public PacsStudyImportRequestListener(final PacsService pacsService, final MailService mailService, final SiteConfigPreferences preferences) {
        _pacsService = pacsService;
        _mailService = mailService;
        _preferences = preferences;
    }

    @JmsListener(id = "pacsStudyImportRequest", destination = "pacsStudyImportRequest")
    public void onPacsStudyImportRequest(final PacsStudyImportRequest pacsStudyImportRequest) throws Exception {
        try {
            //Study import requests are not currently set up to allow users to specify which AE to send the data to
            log.info("Listener received study import request: {}", pacsStudyImportRequest);
            for (final PacsSeriesImportRequest pacsSeriesImportRequest : pacsStudyImportRequest.getSeries()) {
                _pacsService.importSeries(pacsStudyImportRequest.getRequestingUser(), pacsStudyImportRequest.getPacs(),
                                          pacsSeriesImportRequest.getStudy(), pacsSeriesImportRequest.getSeries(), null);
            }
            sendCompleteNotification(pacsStudyImportRequest);
            log.info("Listener completed study import request");
        } catch (final Exception e) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request {} with an unexpected error", pacsStudyImportRequest, e);
            throw e;
        }
    }

    private void sendCompleteNotification(final PacsStudyImportRequest request) throws Exception {
        // refresh the user, just in case their email has changed since they made the request
        try {
            final XDATUser user = new XDATUser(request.getRequestingUser().getUsername());
            _mailService.sendMessage(_preferences.getAdminEmail(), user.getEmail(),
                                     "[" + TurbineUtils.GetSystemName() + "] PACS Study Import Request Complete",
                                     "The study you requested from the PACS has been successfully imported.");
            final EventDetails        eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_COMPLETE");
            final PersistentWorkflowI workflow     = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, request.getStudy().getStudyId(), request.getStudy().getProjectId(), eventDetails);
            assert workflow != null;
            PersistentWorkflowUtils.complete(workflow, workflow.buildEvent());
        } catch (final UserNotFoundException e) {
            // not much to do here - was their account deleted since they made the request?
            log.error("User {} queued up a PACS import request, but their user account cannot be found to send them a confirmation email.", request.getRequestingUser().getUsername());
        }
    }

    private final PacsService           _pacsService;
    private final MailService           _mailService;
    private final SiteConfigPreferences _preferences;
}
