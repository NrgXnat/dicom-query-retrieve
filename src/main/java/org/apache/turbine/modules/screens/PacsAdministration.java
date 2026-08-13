/*
 * dicom-query-retrieve: org.apache.turbine.modules.screens.PacsAdministration
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.screens;

import org.apache.turbine.util.RunData;
import org.apache.turbine.pipeline.PipelineData;
import org.apache.velocity.context.Context;

@SuppressWarnings("unused")
public class PacsAdministration extends DqrSecureScreen {
    @Override
    protected void doBuildTemplate(final PipelineData pipelineData, final Context context) {
        final RunData data = pipelineData.getRunData();
        super.doBuildTemplate(data, context);
        context.put("strategies", getOrmStrategyMap().keySet());
    }
}
