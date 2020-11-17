/*
 * DqrSecureAction
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.modules.actions;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.turbine.util.RunData;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.DqrAdminSettingsForProjectService;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.dqr.services.PacsService;
import org.nrg.mail.services.MailService;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;

@Getter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
public abstract class DqrSecureAction extends SecureAction {
    public final static String PACS_SESSION_KEY  = "pacs";
    public final static String STUDY_SESSION_KEY = "study";

    protected DqrSecureAction() {
        _pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        _pacsService = XDAT.getContextService().getBean(PacsService.class);
        _dqrPreferences = XDAT.getContextService().getBean(DqrPreferences.class);
        _siteConfigPreferences = XDAT.getContextService().getBean(SiteConfigPreferences.class);
        _dqrAdminSettings = XDAT.getContextService().getBean(DqrAdminSettingsForProjectService.class);
        _mailService = XDAT.getContextService().getBean(MailService.class);
    }

    public Pacs getPassedPacs(final RunData data) throws PacsNotFoundException, DataFormatException {
        final long pacsId = getPassedPacsId(data);
        final Pacs pacs   = getPacsEntityService().retrieve(pacsId);
        if (null == pacs) {
            throw new PacsNotFoundException(pacsId);
        }
        return pacs;
    }

    public Pacs getPacsFromSession(final RunData data) {
        return (Pacs) data.getSession().getAttribute(PACS_SESSION_KEY);
    }

    public Study getStudyFromSession(final RunData data) {
        return (Study) data.getSession().getAttribute(STUDY_SESSION_KEY);
    }

    public void setDqrSessionVariables(final RunData data, final Pacs pacs, final Study study) {
        data.getSession().setAttribute(PACS_SESSION_KEY, pacs);
        data.getSession().setAttribute(STUDY_SESSION_KEY, study);
    }

    public static void removeDqrSessionVariables(final RunData data) {
        data.getSession().removeAttribute(PACS_SESSION_KEY);
        data.getSession().removeAttribute(STUDY_SESSION_KEY);
    }

    private Long getPassedPacsId(final RunData data) throws DataFormatException {
        final String pacsId = (String) TurbineUtils.GetPassedParameter("pacsId", data);
        try {
            return Long.valueOf(pacsId);
        } catch (NumberFormatException e) {
            throw new DataFormatException(pacsId);
        }
    }

    private final PacsEntityService                 _pacsEntityService;
    private final PacsService                       _pacsService;
    private final DqrPreferences                    _dqrPreferences;
    private final SiteConfigPreferences             _siteConfigPreferences;
    private final DqrAdminSettingsForProjectService _dqrAdminSettings;
    private final MailService                       _mailService;
}
