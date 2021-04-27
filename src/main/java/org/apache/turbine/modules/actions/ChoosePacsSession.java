/*
 * dicom-query-retrieve: org.apache.turbine.modules.actions.ChoosePacsSession
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */


package org.apache.turbine.modules.actions;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;

import java.util.Optional;

@SuppressWarnings("unused")
public class ChoosePacsSession extends DqrSecureAction {
    @Override
    public void doPerform(final RunData data, final Context context) throws PacsNotFoundException, PacsNotQueryableException, NotFoundException {
        final Pacs            pacs             = getPassedPacs(data);
        final String          studyInstanceUid = (String) TurbineUtils.GetPassedParameter("studyInstanceUid", data);
        final Optional<Study> study            = getPacsService().getStudyById(XDAT.getUserDetails(), pacs, studyInstanceUid);
        setDqrSessionVariables(data, pacs, study.orElseThrow(() -> new NotFoundException("No study was found with the ID " + studyInstanceUid + " on the PACS " + pacs.getId())));
        context.put("study", study.get());
        context.put("pacs", pacs);
        data.setScreenTemplate("PacsSessionFinder2.vm");
    }
}
