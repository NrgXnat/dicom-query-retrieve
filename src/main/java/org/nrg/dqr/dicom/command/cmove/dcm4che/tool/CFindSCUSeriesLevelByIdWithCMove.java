/*
 * CFindSCUSeriesLevelByIdWithCMove
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dicom.command.cmove.dcm4che.tool;

import org.dcm4che2.tool.dcmqr.DcmQR;
import org.nrg.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSeriesLevelById;
import org.nrg.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.dqr.preferences.DqrPreferences;

public class CFindSCUSeriesLevelByIdWithCMove extends CFindSCUSeriesLevelById {

    public CFindSCUSeriesLevelByIdWithCMove(final DqrPreferences preferences, DicomConnectionProperties dicomConnectionProperties, CEchoSCU cechoSCU,
                                            OrmStrategy ormStrategy) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected boolean cMoveRequestedOnResults() {
        return true;
    }

    @Override
    protected DcmQR createDcmQR(String localAETitle) {
        return new DqrCMoveDcmQR(localAETitle);
    }
}
