package org.nrg.xnatx.dqr.messaging;

import lombok.extern.slf4j.Slf4j;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xnatx.dqr.services.PacsService;

@Slf4j
public class AbstractPacsRequestListener {
    protected String getAdminEmail() {
        return getSiteConfigPreferences().getAdminEmail();
    }

    protected PacsService getPacsService() {
        if (_pacsService == null) {
            _pacsService = XDAT.getContextService().getBean(PacsService.class);
        }
        return _pacsService;
    }

    protected SiteConfigPreferences getSiteConfigPreferences() {
        if (_siteConfigPreferences == null) {
            _siteConfigPreferences = XDAT.getSiteConfigPreferences();
        }
        return _siteConfigPreferences;
    }

    protected MailService getMailService() {
        if (_mailService == null) {
            _mailService = XDAT.getMailService();
        }
        return _mailService;
    }

    private PacsService           _pacsService;
    private SiteConfigPreferences _siteConfigPreferences;
    private static MailService           _mailService;
}
