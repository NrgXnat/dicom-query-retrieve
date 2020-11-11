/*
 * PacsSessionFinder
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.screens;

import org.apache.turbine.modules.actions.DqrSecureAction;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class SpreadsheetBasedImporter extends DqrSecureScreen {
    @Override
    protected void doBuildTemplate(final RunData data, final Context context) {
        final List<Pacs> pacsList = getPacsEntityService().findAllQueryable();
        if (pacsList.isEmpty()) {
            data.setScreenTemplate("PacsSessionFinderNoPacsFound.vm");
        } else {
            context.put("pacsList", pacsList);
        }
        DqrSecureAction.removeDqrSessionVariables(data);

        final Collection<DicomSCPInstance> scps = getDicomScpManager().getDicomSCPInstances().values();
        context.put("scps", scps);
        context.put("enabledScps", scps.stream().filter(DicomSCPInstance::isEnabled).collect(Collectors.toList()));
        context.put("project", XnatProjectdata.getXnatProjectdatasById(TurbineUtils.GetPassedParameter("project", data), null, false));
    }
}
