/*
 * ChoosePacsSession
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

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;

@SuppressWarnings("unused")
public class ChoosePacsSession extends DqrSecureAction {
    public ChoosePacsSession() {
        super();
    }

    @Override
    public void doPerform(final RunData data, final Context context) throws PacsNotFoundException, DataFormatException {
        final String studyInstanceUid = ((String) TurbineUtils.GetPassedParameter("studyInstanceUid", data));
        final Pacs   pacs             = getPassedPacs(data);
        final Study  study            = getPacsService().getStudyById(getUser(), pacs, studyInstanceUid);
        setDqrSessionVariables(data, pacs, study);
        context.put(STUDY_SESSION_KEY, study);
        context.put(PACS_SESSION_KEY, pacs);
        data.setScreenTemplate("PacsSessionFinder2.vm");
    }
}
