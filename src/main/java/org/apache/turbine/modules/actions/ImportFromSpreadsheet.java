/*
 * dicom-query-retrieve: org.apache.turbine.modules.actions.ImportFromSpreadsheet
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.actions;

import java.io.File;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.xapi.exceptions.NotAuthenticatedException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.Pacs;

@SuppressWarnings("unused")
@Slf4j
public class ImportFromSpreadsheet extends DqrSecureAction {
    @Override
    public void doPerform(final RunData data, final Context context) throws Exception {
        UserI user = XDAT.getUserDetails();
        if (user.isGuest()) {
            throw new NotAuthenticatedException("");
        } else if (!Roles.checkRole(user, "Dqr") && !Roles.checkRole(user, "Administrator") && !getDqrPreferences().getAllowAllUsersToUseDqr()) {
            throw new RuntimeException("You do not have access to DQR functionality.");
        }

        final String project = (String) TurbineUtils.GetPassedParameter("project", data);
        if (!getDqrPreferences().getAllowAllProjectsToUseDqr() && !getDqrAdminSettings().isDqrEnabledForProject(project)) {
            //You cannot import into a project that does not have DQR enabled.
            throw new RuntimeException("You cannot import into a project that does not have DQR enabled.");
        }
        final XnatProjectdata projectObject = XnatProjectdata.getXnatProjectdatasById(project, null, false);
        if (!projectObject.canEdit(user)) {
            throw new RuntimeException("You do not have access to this session.");
        }
        final ParameterParser params = data.getParameters();
        //grab the FileItems available in ParameterParser
        final FileItem fileItem = params.getFileItem("csv_to_store");
        final File     temp     = File.createTempFile("xnat", "csv");
        fileItem.write(temp);

        final String ae   = (String) TurbineUtils.GetPassedParameter("ae", data);
        final Pacs   pacs = getPassedPacs(data);
        getPacsService().processSpreadsheetImport(user, temp, ae, project, pacs.getId());

        temp.delete();
        fileItem.delete();
        data.setScreenTemplate("XDATScreen_prearchives.vm");
    }
}
