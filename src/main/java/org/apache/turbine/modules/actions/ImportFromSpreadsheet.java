/*
 * dicom-query-retrieve: org.apache.turbine.modules.actions.ImportFromSpreadsheet
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.actions;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.fileupload.FileItem;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.xapi.exceptions.InitializationException;
import org.nrg.xapi.exceptions.InsufficientPrivilegesException;
import org.nrg.xapi.exceptions.NotAuthenticatedException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.ProjectNotDqrEnabledException;

import java.io.File;

@SuppressWarnings("unused")
@Slf4j
public class ImportFromSpreadsheet extends DqrSecureAction {
    @Override
    public void doPerform(final RunData data, final Context context) throws InsufficientPrivilegesException, NotFoundException, ProjectNotDqrEnabledException, InitializationException, NotAuthenticatedException, PacsNotFoundException {
        final UserI user = XDAT.getUserDetails();
        if (user.isGuest()) {
            throw new NotAuthenticatedException("");
        }
        if (!Roles.checkRole(user, "Dqr") && !Roles.checkRole(user, "Administrator") && !getDqrPreferences().getAllowAllUsersToUseDqr()) {
            throw new InsufficientPrivilegesException(user.getUsername(), "You do not have access to DQR functionality.");
        }

        final String projectId = (String) TurbineUtils.GetPassedParameter("project", data);
        if (!getDqrPreferences().getAllowAllProjectsToUseDqr() && !getDqrAdminSettings().isDqrEnabledForProject(projectId)) {
            //You cannot import into a project that does not have DQR enabled.
            throw new ProjectNotDqrEnabledException(projectId);
        }

        final XnatProjectdata project = XnatProjectdata.getXnatProjectdatasById(projectId, null, false);
        final boolean         canRead;
        try {
            canRead = project.canRead(user);
        } catch (Exception e) {
            throw new InitializationException("Error checking permissions on project " + projectId + " for user " + user.getUsername(), e);
        }
        if (!canRead) {
            throw new InsufficientPrivilegesException(user.getUsername(), "You do not have access to data in the project " + projectId);
        }

        final Pacs pacs = getPassedPacs(data);

        final ParameterParser parameters = data.getParameters();
        //grab the FileItems available in ParameterParser
        final FileItem fileItem = parameters.getFileItem("csv_to_store");
        final File     temp;
        try {
            temp = File.createTempFile("xnat", "csv");
            fileItem.write(temp);
        } catch (Exception e) {
            throw new DqrRuntimeException("An error occurred trying to write the incoming CSV file locally", e);
        }

        final String ae = (String) TurbineUtils.GetPassedParameter("ae", data);
        getDicomQueryRetrieveService().processSpreadsheetImport(user, temp, ae, projectId, pacs.getId());

        temp.delete();
        fileItem.delete();
        data.setScreenTemplate("XDATScreen_prearchives.vm");
    }
}
