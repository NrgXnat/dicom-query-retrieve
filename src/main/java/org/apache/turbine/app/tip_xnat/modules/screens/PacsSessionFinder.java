/*
 * PacsSessionFinder
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.apache.turbine.app.tip_xnat.modules.screens;

import java.util.List;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.tip.domain.entities.Pacs;
import org.nrg.tip.services.PacsEntityService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class PacsSessionFinder extends SecureScreen {

    @Override
    protected void doBuildTemplate(final RunData data, final Context context) throws Exception {
        // no-op so that SecureScreen.doBuildTemplate fires...
        PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        List<Pacs> pacsList = pacsEntityService.getAll();
        if (pacsList.isEmpty()) {
            data.setScreenTemplate("PacsSessionFinderNoPacsFound.vm");
        } else {
            context.put("pacsList", pacsList);
        }
        org.apache.turbine.app.tip_xnat.modules.actions.TipSecureAction.removeTipSessionVariables(data);
    }
}
