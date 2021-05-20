/*
 * dicom-query-retrieve: org.apache.turbine.modules.screens.ExportSessionToPacs
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.screens;

import java.util.List;
import org.apache.commons.lang3.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xnatx.dqr.domain.entities.Pacs;

@SuppressWarnings("unused")
public class ExportSessionToPacs extends DqrSecureScreen {
    @Override
    protected void doBuildTemplate(final RunData data, final Context context) {
        super.doBuildTemplate(data, context);

        final String sessionId = data.getParameters().get("sessionId");
        if (StringUtils.isBlank(sessionId)) {
            context.put("message", "You must specify a valid session ID that you want to export.");
        }

        final List<Pacs> pacsList = getPacsService().findAllStorable();
        if (pacsList == null || pacsList.size() == 0) {
            context.put("message", "No PACS were found configured on this system.");
            return;
        }
        context.put("pacsList", pacsList);

        final XnatImagesessiondata session = XnatImagesessiondata.getXnatImagesessiondatasById(sessionId, XDAT.getUserDetails(), false);
        if (session == null) {
            context.put("message", "There was no session associated with the requested session ID: " + sessionId);
            return;
        }
        context.put("session", session);
        context.put("om", session);
        context.put("subject", session.getSubjectData());
        context.put("projectId", StringUtils.defaultIfBlank(data.getParameters().get("project"), session.getProject()));

        final List<XnatImagescandataI> scans = session.getScans_scan();
        if (scans == null || scans.size() == 0) {
            context.put("message", "There were no scans found associated with the requested session.");
        } else {
            context.put("scans", scans);
        }
    }
}
