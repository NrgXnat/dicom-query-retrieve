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
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;
import org.nrg.xdat.turbine.utils.TurbineUtils;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;

public class PacsSessionFinder2 extends SecureScreen {

    @Override
    protected void doBuildTemplate(RunData data, Context context) throws Exception {
        ArrayList<XnatProjectdata> projects = XnatProjectdata.getAllXnatProjectdatas(TurbineUtils.getUser(data), true);
        Comparator<XnatProjectdata> projRunningTitleComparator
                = new Comparator<XnatProjectdata>() {
            public int compare(XnatProjectdata p1, XnatProjectdata p2) {
                String run1 = p1.getSecondaryId().toUpperCase();
                String run2 = p2.getSecondaryId().toUpperCase();
                return run1.compareTo(run2);
            }

        };
        Collections.sort(projects,projRunningTitleComparator);
        context.put("projects", projects);
    }

}
