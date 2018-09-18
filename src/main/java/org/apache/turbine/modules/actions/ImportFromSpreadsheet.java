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
import org.apache.turbine.util.parser.ParameterParser;
import org.apache.velocity.context.Context;
import org.nrg.dqr.services.PacsService;
import org.nrg.xapi.exceptions.NotAuthenticatedException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.security.UserI;

import java.io.File;

@SuppressWarnings("unused")
@Slf4j
public class ImportFromSpreadsheet extends DqrSecureAction {
    private PacsService _service;

    @Override
    public void doPerform(final RunData data, final Context context) throws Exception {
        final UserI user = XDAT.getUserDetails();
        if(user == null || user.isGuest()){
            throw new NotAuthenticatedException("");
        }
        ParameterParser params = data.getParameters();

        //grab the FileItems available in ParameterParser
        FileItem fi = params.getFileItem("csv_to_store");
        File temp = File.createTempFile("xnat", "csv");
        fi.write(temp);

        final String ae = (String) TurbineUtils.GetPassedParameter("ae", data);
        final String project = (String) TurbineUtils.GetPassedParameter("project", data);
        final long pacsId = Long.valueOf((String) TurbineUtils.GetPassedParameter("pacsId", data));

        _service = XDAT.getContextService().getBean(PacsService.class);
        _service.processSpreadsheetImport(user,  temp, ae, project, pacsId);

        temp.delete();
        fi.delete();
	data.setScreenTemplate("XDATScreen_prearchives.vm");
    }
}
