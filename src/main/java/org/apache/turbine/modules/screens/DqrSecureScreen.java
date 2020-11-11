/*
 * dicom-query-retrieve: org.apache.turbine.modules.screens.DqrSecureScreen
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.apache.turbine.modules.screens;

import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.turbine.util.RunData;
import org.apache.velocity.context.Context;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.turbine.modules.screens.SecureScreen;

@Getter(AccessLevel.PROTECTED)
@Accessors(prefix = "_")
public abstract class DqrSecureScreen extends SecureScreen {
    protected DqrSecureScreen() {
        _pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        _dicomScpManager = XDAT.getContextService().getBean(DicomSCPManager.class);
    }

    @Override
    protected abstract void doBuildTemplate(final RunData runData, final Context context) throws Exception;

    private final PacsEntityService _pacsEntityService;
    private final DicomSCPManager   _dicomScpManager;
}
