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
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.services.PacsService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.annotation.JmsListener;
import org.springframework.stereotype.Component;

import java.util.stream.Collectors;

@Component
@Slf4j
public class PacsStudyImportRequestListener extends PacsRequestListener {
    @Autowired
    public PacsStudyImportRequestListener(final PacsService pacsService, final MailService mailService, final SiteConfigPreferences preferences) {
        super(pacsService, mailService, preferences, IMPORT_SUBJECT, IMPORT_MESSAGE, IMPORT_ACTION);
    }

    @JmsListener(id = "pacsStudyImportRequest", destination = "pacsStudyImportRequest")
    public void onPacsStudyImportRequest(final PacsStudyImportRequest request) throws Exception {
        try {
            // Study import requests are not currently set up to allow users to specify which AE to send the data to
            log.info("Listener received study import request: {}", request);
            for (final PacsSeriesImportRequest pacsSeriesImportRequest : request.getSeries()) {
                getPacsService().importSeries(request.getRequestingUser(), request.getPacs(), pacsSeriesImportRequest.getStudy(), pacsSeriesImportRequest.getSeries(), null);
            }
            sendCompleteNotification(request.getRequestingUser(), request.getStudy().getStudyId(), request.getStudy().getProjectId(), "Series: " + request.getSeries().stream().map(PacsSeriesImportRequest::getSeries).map(Series::getSeriesInstanceUid).collect(Collectors.joining(", ")));
            log.info("Listener completed study import request");
        } catch (final Exception e) {
            // If errors are not logged before they're rethrown, they do not show up in any of the files
            log.error("Choked on request {} with an unexpected error", request, e);
            throw e;
        }
    }

    private static final String IMPORT_SUBJECT = "[%s] PACS Study Import Request Complete";
    private static final String IMPORT_ACTION  = "IMPORT_FROM_PACS_COMPLETE";
    private static final String IMPORT_MESSAGE = "The study you requested from the PACS has been successfully imported";
}
