/*
 * PacsAdministration
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
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xdat.XDAT;

import java.util.Map;
import java.util.stream.Collectors;

@SuppressWarnings("unused")
public class PacsAdministration extends DqrSecureScreen {
    public PacsAdministration() {
        _strategies = XDAT.getContextService().getBeansOfType(OrmStrategy.class).entrySet().stream().collect(Collectors.toMap(Map.Entry::getValue, Map.Entry::getKey));
    }

    @Override
    protected void doBuildTemplate(final RunData data, final Context context) {
        context.put("strategies", _strategies);
    }

    private final Map<OrmStrategy, String> _strategies;
}
