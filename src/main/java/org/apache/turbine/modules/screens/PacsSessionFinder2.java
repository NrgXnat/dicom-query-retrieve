/*
 * PacsSessionFinder2
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

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class PacsSessionFinder2 extends DqrSecureScreen {
    @Override
    protected void doBuildTemplate(final RunData data, final Context context) {
        context.put("project", XnatProjectdata.getXnatProjectdatasById(TurbineUtils.GetPassedParameter("project", data), null, false));
        context.put("enabledScps", getDicomScpManager().getDicomSCPInstances().values().stream().filter(DicomSCPInstance::isEnabled).collect(Collectors.toList()));
    }
}
