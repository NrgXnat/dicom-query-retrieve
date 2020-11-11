/*
 * ExportSessionToPacsRequested
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

import lombok.extern.slf4j.Slf4j;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;

@Slf4j
@SuppressWarnings("unused")
public class ExportSessionToPacsRequested extends DqrSecureScreen {
    @Override
    protected void doBuildTemplate(final RunData data, final Context context) {
        log.info("There was a request to export a session to a PACS apparently.");
    }
}
