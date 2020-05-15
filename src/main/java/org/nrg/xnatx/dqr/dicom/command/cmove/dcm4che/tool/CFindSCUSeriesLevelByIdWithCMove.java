/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.CFindSCUSeriesLevelByIdWithCMove
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool;

import org.dcm4che2.tool.dcmqr.DcmQR;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUSeriesLevelById;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

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
