package org.nrg.xnatx.dqr.messaging;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.nrg.framework.messaging.JmsRequestListener;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xnatx.dqr.services.PacsService;

@Getter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
@Slf4j
public abstract class AbstractPacsRequestListener<T> implements JmsRequestListener<T> {
    protected AbstractPacsRequestListener(final PacsService pacsService, final SiteConfigPreferences siteConfigPreferences, final MailService mailService) {
        _pacsService = pacsService;
        _siteConfigPreferences = siteConfigPreferences;
        _mailService = mailService;
    }

    public abstract void onRequest(final T request) throws Exception;

    protected String getAdminEmail() {
        return getSiteConfigPreferences().getAdminEmail();
    }

    private final PacsService           _pacsService;
    private final SiteConfigPreferences _siteConfigPreferences;
    private final MailService           _mailService;
}
