/*
 * dicom-query-retrieve: org.apache.turbine.modules.screens.PacsSessionFinder2
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

@SuppressWarnings("unused")
public class PacsSessionFinder2 extends DqrSecureScreen {
    @Override
    protected void doBuildTemplate(RunData data, Context context) {
        super.doBuildTemplate(data, context);
        storeProjectAndQueryablePacs(data, context);
    }
}
