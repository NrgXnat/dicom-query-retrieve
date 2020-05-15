/*
 * dicom-query-retrieve: org.apache.turbine.modules.screens.ExportSessionToPacsRequested
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
public class ExportSessionToPacsRequested extends DqrSecureScreen {
    @Override
    protected void doBuildTemplate(final RunData data, final Context context) {
        // no-op so that SecureScreen.doBuildTemplate fires...
    }
}
