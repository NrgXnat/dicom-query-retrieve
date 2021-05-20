/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.AbstractPacsRequestListener
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.messaging.JmsRequestListener;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;

@Getter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
@Slf4j
public abstract class AbstractPacsRequestListener<T> implements JmsRequestListener<T> {
    protected AbstractPacsRequestListener(final DicomQueryRetrieveService dqrService, final SiteConfigPreferences siteConfigPreferences, final MailService mailService) {
        _dqrService = dqrService;
        _siteConfigPreferences = siteConfigPreferences;
        _mailService = mailService;
    }

    public abstract void onRequest(final T request) throws Exception;

    protected String getAdminEmail() {
        return getSiteConfigPreferences().getAdminEmail();
    }

    private final DicomQueryRetrieveService _dqrService;
    private final SiteConfigPreferences     _siteConfigPreferences;
    private final MailService           _mailService;
}
