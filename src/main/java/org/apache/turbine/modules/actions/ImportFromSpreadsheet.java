/*
 * ExportSessionToPacs
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

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.exceptions.NotAuthenticatedException;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;

import java.io.File;

@Slf4j
@SuppressWarnings("unused")
public class ImportFromSpreadsheet extends DqrSecureAction {
    @Override
    public void doPerform(final RunData data, final Context context) throws Exception {
        final UserI user = getUser();
        if (user.isGuest()) {
            throw new NotAuthenticatedException("");
        }
        if (!Roles.checkRole(user, "Dqr") && !Roles.checkRole(user, "Administrator") && !getDqrPreferences().getAllowAllUsersToUseDqr()) {
            throw new InsufficientPrivilegesException("You do not have access to DQR functionality.");
        }

        final String ae      = (String) TurbineUtils.GetPassedParameter("ae", data);
        final String project = (String) TurbineUtils.GetPassedParameter("project", data);
        final long   pacsId  = Long.parseLong((String) TurbineUtils.GetPassedParameter("pacsId", data));

        if (!getDqrPreferences().getAllowAllProjectsToUseDqr() && !getDqrAdminSettings().isDqrEnabledForProject(project)) {
            //You cannot import into a project that does not have DQR enabled.
            throw new RuntimeException("You cannot import into a project that does not have DQR enabled.");
        }
        final XnatProjectdata projectObject = XnatProjectdata.getXnatProjectdatasById(project, null, false);
        if (!projectObject.canEdit(user)) {
            throw new RuntimeException("You do not have access to this session.");
        }

        //grab the FileItems available in ParameterParser
        final File     temp     = File.createTempFile("xnat", "csv");
        final FileItem fileItem = data.getParameters().getFileItem("csv_to_store");
        fileItem.write(temp);

        getPacsService().processSpreadsheetImport(user, temp, ae, project, pacsId);

        temp.delete();
        fileItem.delete();

        data.setScreenTemplate("XDATScreen_prearchives.vm");
    }
}
