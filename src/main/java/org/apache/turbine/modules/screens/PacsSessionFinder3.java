/*
 * PacsSessionFinder3
 * TIP is developed by the Neuroinformatics Research Group
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
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

public class PacsSessionFinder3 extends SecureScreen {

    @Override
    protected void doBuildTemplate(RunData arg0, Context arg1) throws Exception {
        // no-op so that SecureScreen.doBuildTemplate fires...
    }
}
