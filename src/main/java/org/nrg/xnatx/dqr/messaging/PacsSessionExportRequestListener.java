/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsSessionExportRequestListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.extern.slf4j.Slf4j;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;
import org.nrg.xnatx.dqr.services.PacsService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

@Component
@Slf4j
public class PacsSessionExportRequestListener extends AbstractPacsRequestListener<PacsSessionExportRequest> {
    @Autowired
    public PacsSessionExportRequestListener(final PacsService pacsService, final SiteConfigPreferences siteConfigPreferences, final MailService mailService) {
        super(pacsService, siteConfigPreferences, mailService);
    }

    @JmsListener(id = "pacsStudyExportRequest", destination = "pacsStudyExportRequest")
    public void onRequest(final PacsSessionExportRequest request) throws Exception {
        try {
            log.info("Listener received session export request from user {}", request.getRequestingUser());

            final Pacs pacsToExportTo = request.getPacs();
            if (!pacsToExportTo.isStorable()) {
                throw new PacsNotStorableException(request.getPacs().getId());
            }

            final XDATUser user = new XDATUser(request.getRequestingUser());
            for (final PacsScanExportRequest scan : request.getScans()) {
                getPacsService().exportSeries(user, pacsToExportTo, XnatImagescandata.getXnatImagescandatasByXnatImagescandataId(scan.getImageScanDataId(), user, false));
            }

            // Send complete notification
            getMailService().sendMessage(getAdminEmail(), user.getEmail(), "[" + TurbineUtils.GetSystemName() + "] PACS Session Export Request Complete", "The session you requested has been successfully exported to the PACS.");

            log.info("Listener completed session export request from user {}", request.getRequestingUser());
        } catch (final UserNotFoundException e) {
            // not much to do here - was their account deleted since they made the request?
            log.error("User {} queued up a PACS export request, but their user account cannot be found to send them a confirmation email.", request.getRequestingUser());
        } catch (final Exception e) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request {} with the following error", request, e);
            throw e;
        }
    }
}
