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

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.services.PacsService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.extensions.PacsNotStorableException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Slf4j
public class PacsSessionExportRequestListener extends PacsRequestListener {
    @Autowired
    public PacsSessionExportRequestListener(final PacsService pacsService, final SiteConfigPreferences preferences, final MailService mailService) {
        super(pacsService, mailService, preferences, EXPORT_SUBJECT, EXPORT_MESSAGE, EXPORT_ACTION);
    }

    @JmsListener(id = "pacsStudyExportRequest", destination = "pacsStudyExportRequest")
    public void onPacsSessionExportRequest(final PacsSessionExportRequest request) throws Exception {
        try {
            log.info("Listener received session export request from user {} to retrieve the following scans from session {}: {}", request.getUsername(), request.getSessionId(), request.getScans().stream().map(PacsScanExportRequest::getScanId).collect(Collectors.joining(", ")));
            final Pacs pacsToExportTo = request.getPacs();
            if (pacsToExportTo.isStorable()) {
                throw new PacsNotStorableException(pacsToExportTo.getId());
            }
            final UserI                user    = Users.getUser(request.getUsername());
            final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(request.getSessionId(), user, false);
            for (final PacsScanExportRequest pacsScanExportRequest : request.getScans()) {
                getPacsService().exportSeries(user, pacsToExportTo, session.getScanById(pacsScanExportRequest.getScanId()));
            }
            sendCompleteNotification(user, request.getSessionId(), session.getProject(), "Pacs: " + pacsToExportTo.getId());
        } catch (final Exception e) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request {} with the following error", request, e);
            throw e;
        }
    }

    private static final String EXPORT_SUBJECT = "[%s] PACS Study Import Request Complete";
    private static final String EXPORT_ACTION  = "EXPORT_TO_PACS_COMPLETE";
    private static final String EXPORT_MESSAGE = "The session you requested has been successfully exported to the PACS";
}
