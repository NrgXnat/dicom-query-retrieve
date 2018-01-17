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

import java.util.List;

import org.apache.turbine.modules.actions.DqrSecureAction;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class PacsSessionFinder extends SecureScreen {

    @Override
    protected void doBuildTemplate(final RunData data, final Context context) throws Exception {
        // no-op so that SecureScreen.doBuildTemplate fires...
        PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        List<Pacs> pacsList = pacsEntityService.findAllQueryable();
        if (pacsList.isEmpty()) {
            data.setScreenTemplate("PacsSessionFinderNoPacsFound.vm");
        } else {
            context.put("pacsList", pacsList);
        }
        DqrSecureAction.removeDqrSessionVariables(data);
    }
}
