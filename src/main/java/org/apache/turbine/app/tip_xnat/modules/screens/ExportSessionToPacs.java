/*
 * org.apache.turbine.app.tip_xnat.modules.screens.ExportSessionToPacs
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 11/20/13 11:21 AM
 */

package org.apache.turbine.app.tip_xnat.modules.screens;

import org.apache.commons.lang.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.tip.domain.entities.Pacs;
import org.nrg.tip.services.PacsEntityService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.ArrayList;
import java.util.List;

public class ExportSessionToPacs extends SecureScreen {

    @Override
    protected void doBuildTemplate(final RunData data, final Context context) throws Exception {
        final String sessionId = data.getParameters().get("sessionId");
        if (StringUtils.isBlank(sessionId)) {
            context.put("message", "You must specify a valid session ID that you want to export.");
        }

        final List<Pacs> pacsList = XDAT.getContextService().getBean(PacsEntityService.class).getAll();
        if (pacsList == null || pacsList.size() == 0) {
            context.put("message", "No PACS were found configured on this system.");
            return;
        }
        context.put("pacsList", pacsList);

        final XnatMrsessiondata session = XnatMrsessiondata.getXnatMrsessiondatasById(sessionId, TurbineUtils.getUser(data), false);
        if (session == null) {
            context.put("message", "There was no session associated with the requested session ID: " + sessionId);
            return;
        }
        context.put("session", session);

        final List<XnatImagescandataI> scans = session.getScans_scan();
        if (scans == null || scans.size() == 0) {
            context.put("message", "There were no scans found associated with the requested session.");
        } else {
            context.put("scans", scans);
        }
    }
}
