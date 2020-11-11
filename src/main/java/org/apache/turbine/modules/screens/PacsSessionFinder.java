/*
 * PacsSessionFinder
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.modules.screens;

import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.modules.actions.DqrSecureAction;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.List;

@SuppressWarnings("unused")
public class PacsSessionFinder extends DqrSecureScreen {
    @Override
    protected void doBuildTemplate(final RunData data, final Context context) {
        final List<Pacs> pacsList = getPacsEntityService().findAllQueryable();
        if (pacsList.isEmpty()) {
            data.setScreenTemplate("PacsSessionFinderNoPacsFound.vm");
        } else {
            context.put("pacsList", pacsList);
        }
        DqrSecureAction.removeDqrSessionVariables(data);
        context.put("project", XnatProjectdata.getXnatProjectdatasById(TurbineUtils.GetPassedParameter("project", data), null, false));
        //noinspection InstantiationOfUtilityClass
        context.put("stringUtils", new StringUtils());
    }
}
