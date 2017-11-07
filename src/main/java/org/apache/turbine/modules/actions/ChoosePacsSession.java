/*
 * ChoosePacsSession
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */


package org.apache.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.tip.domain.Study;
import org.nrg.tip.domain.entities.Pacs;
import org.nrg.tip.services.PacsService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;

public class ChoosePacsSession extends TipSecureAction {

    @Override
    public void doPerform(final RunData data, final Context context) throws PacsNotFoundException {

        final String studyInstanceUid = ((String) TurbineUtils.GetPassedParameter("studyInstanceUid", data));
        final PacsService pacsService = XDAT.getContextService().getBean(PacsService.class);
        final Pacs pacs = getPassedPacs(data);
        final Study study = pacsService.getStudyById(XDAT.getUserDetails(), pacs, studyInstanceUid);
        setTipSessionVariables(data, pacs, study);
        context.put("study", study);
        context.put("pacs", pacs);
        data.setScreenTemplate("PacsSessionFinder2.vm");
    }
}
