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

@SuppressWarnings("unused")
public class PacsSessionFinder2 extends DqrSecureScreen {
    @Override
    protected void doBuildTemplate(RunData data, Context context) {
        storeProjectAndQueryablePacs(data, context);
    }
}
