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

import com.google.common.base.Joiner;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.lang.StringUtils;
import org.apache.turbine.util.RunData;
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.dqr.dicom.command.cfind.SearchCriteriaTooVagueException;
import org.nrg.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.DqrAdminSettingsForProject;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.restlet.InvalidStudyDateRangeException;
import org.nrg.dqr.restlet.JsonViews;
import org.nrg.dqr.restlet.RequestUtils;
import org.nrg.dqr.services.*;
import org.nrg.xapi.exceptions.NotAuthenticatedException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.*;
import org.nrg.xdat.security.helpers.Permissions;
import org.nrg.xdat.security.helpers.Roles;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.restlet.extensions.*;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.PermissionDeniedDataAccessException;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

public class ImportFromSpreadsheet extends DqrSecureAction {

    private static final Logger _log = LoggerFactory.getLogger(ImportFromSpreadsheet.class);

    private PacsService _service;

    @Override
    public void doPerform(final RunData data, final Context context) throws Exception {

        UserI user = TurbineUtils.getUser(data);
        if(user.isGuest()){
            throw new NotAuthenticatedException("");
        }
        else if(!Roles.checkRole(user,"Dqr") && !Roles.checkRole(user,"Administrator") && !XDAT.getContextService().getBean(DqrPreferences.class).getAllowAllUsersToUseDqr()){
            throw new RuntimeException("You do not have access to DQR functionality.");
        }

        final String ae = (String) TurbineUtils.GetPassedParameter("ae", data);
        final String project = (String) TurbineUtils.GetPassedParameter("project", data);
        final long pacsId = Long.valueOf((String) TurbineUtils.GetPassedParameter("pacsId", data));

        if (!XDAT.getContextService().getBean(DqrAdminSettingsForProjectService.class).isDqrEnabledForProject(project)) {
            //You cannot import into a project that does not have DQR enabled.
            throw new RuntimeException("You cannot import into a project that does not have DQR enabled.");
        }
        final XnatProjectdata projectObject   = XnatProjectdata.getXnatProjectdatasById(project, null, false);
        if (!projectObject.canEdit(user)) {
            throw new RuntimeException("You do not have access to this session.");
        }
        ParameterParser params = data.getParameters();
        //grab the FileItems available in ParameterParser
        FileItem fi = params.getFileItem("csv_to_store");
        File temp = File.createTempFile("xnat", "csv");
        fi.write(temp);

        _service = XDAT.getContextService().getBean(PacsService.class);
        _service.processSpreadsheetImport(user,  temp, ae, project, pacsId);

        temp.delete();
        fi.delete();
	data.setScreenTemplate("XDATScreen_prearchives.vm");
    }
}
