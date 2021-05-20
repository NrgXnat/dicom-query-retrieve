/*
 * dicom-query-retrieve: org.apache.turbine.modules.actions.DqrSecureAction
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.actions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.turbine.util.RunData;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.DqrProjectSettingsService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;

@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
public abstract class DqrSecureAction extends SecureAction {
    public static void removeDqrSessionVariables(final RunData data) {
        data.getSession().removeAttribute(PACS_SESSION_KEY);
        data.getSession().removeAttribute(STUDY_SESSION_KEY);
    }

    protected Pacs getPassedPacs(final RunData data) throws PacsNotFoundException {
        _pacsId = getPassedPacsId(data);
        final Pacs pacs = getPacsService().retrieve(_pacsId);
        if (null == pacs) {
            throw new PacsNotFoundException(_pacsId);
        }
        _pacs = pacs;
        return _pacs;
    }

    protected Pacs getPacsFromSession(final RunData data) {
        return (Pacs) data.getSession().getAttribute(PACS_SESSION_KEY);
    }

    protected Study getStudyFromSession(final RunData data) {
        return (Study) data.getSession().getAttribute(STUDY_SESSION_KEY);
    }

    protected void setDqrSessionVariables(final RunData data, final Pacs pacs, final Study study) {
        data.getSession().setAttribute(PACS_SESSION_KEY, pacs);
        data.getSession().setAttribute(STUDY_SESSION_KEY, study);
    }

    protected DicomQueryRetrieveService getDicomQueryRetrieveService() {
        if (_dqrService == null) {
            _dqrService = XDAT.getContextService().getBean(DicomQueryRetrieveService.class);
        }
        return _dqrService;
    }

    protected PacsService getPacsService() {
        if (_pacsService == null) {
            _pacsService = XDAT.getContextService().getBean(PacsService.class);
        }
        return _pacsService;
    }

    protected DqrPreferences getDqrPreferences() {
        if (_dqrPreferences == null) {
            _dqrPreferences = XDAT.getContextService().getBean(DqrPreferences.class);
        }
        return _dqrPreferences;
    }

    protected DqrProjectSettingsService getDqrAdminSettings() {
        if (_dqrAdminSettings == null) {
            _dqrAdminSettings = XDAT.getContextService().getBean(DqrProjectSettingsService.class);
        }
        return _dqrAdminSettings;
    }

    protected SiteConfigPreferences getSiteConfigPreferences() {
        if (_siteConfigPreferences == null) {
            _siteConfigPreferences = XDAT.getContextService().getBean(SiteConfigPreferences.class);
        }
        return _siteConfigPreferences;
    }

    protected MailService getMailService() {
        if (_mailService == null) {
            _mailService = XDAT.getContextService().getBean(MailService.class);
        }
        return _mailService;
    }

    private static long getPassedPacsId(final RunData data) throws PacsNotFoundException {
        try {
            return Long.parseLong((String) TurbineUtils.GetPassedParameter("pacsId", data));
        } catch (NumberFormatException e) {
            throw new PacsNotFoundException(0);
        }
    }

    private final static String PACS_SESSION_KEY  = "pacs";
    private final static String STUDY_SESSION_KEY = "study";

    private static DicomQueryRetrieveService _dqrService;
    private static PacsService               _pacsService;
    private static DqrPreferences            _dqrPreferences;
    private static DqrProjectSettingsService _dqrAdminSettings;
    private static SiteConfigPreferences     _siteConfigPreferences;
    private static MailService               _mailService;

    private long _pacsId;
    private Pacs _pacs;
}
