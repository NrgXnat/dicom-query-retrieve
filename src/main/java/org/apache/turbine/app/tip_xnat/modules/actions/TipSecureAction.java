/*
 * TipSecureAction
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.app.tip_xnat.modules.actions;

import org.apache.turbine.util.RunData;
import org.nrg.tip.domain.Study;
import org.nrg.tip.domain.entities.Pacs;
import org.nrg.tip.services.PacsEntityService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.modules.actions.SecureAction;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;

public abstract class TipSecureAction extends SecureAction {

    private final static String PACS_SESSION_KEY = "pacs";
    private final static String STUDY_SESSION_KEY = "study";

    public Pacs getPassedPacs(final RunData data) throws PacsNotFoundException {
        PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        Pacs pacs = pacsEntityService.retrieve(getPassedPacsId(data));
        if (null == pacs) {
            throw new PacsNotFoundException();
        } else {
            return pacs;
        }
    }

    private Long getPassedPacsId(final RunData data) throws PacsNotFoundException {
        try {
            return Long.valueOf((String) TurbineUtils.GetPassedParameter("pacsId", data));
        } catch (NumberFormatException e) {
            throw new PacsNotFoundException();
        }
    }

    public Pacs getPacsFromSession(final RunData data) {
        return (Pacs) data.getSession().getAttribute(PACS_SESSION_KEY);
    }

    public Study getStudyFromSession(final RunData data) {
        return (Study) data.getSession().getAttribute(STUDY_SESSION_KEY);
    }

    public void setTipSessionVariables(final RunData data, final Pacs pacs, final Study study) {
        data.getSession().setAttribute(PACS_SESSION_KEY, pacs);
        data.getSession().setAttribute(STUDY_SESSION_KEY, study);
    }

    public static void removeTipSessionVariables(final RunData data) {
        data.getSession().removeAttribute(PACS_SESSION_KEY);
        data.getSession().removeAttribute(STUDY_SESSION_KEY);
    }
}
