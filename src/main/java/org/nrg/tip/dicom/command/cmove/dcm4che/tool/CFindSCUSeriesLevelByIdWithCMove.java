/*
 * org.nrg.tip.dicom.command.cmove.dcm4che.tool.CFindSCUSeriesLevelByIdWithCMove
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.command.cmove.dcm4che.tool;

import org.dcm4che2.tool.dcmqr.DcmQR;
import org.nrg.tip.dicom.command.cecho.CEchoSCU;
import org.nrg.tip.dicom.command.cfind.dcm4che.tool.CFindSCUSeriesLevelById;
import org.nrg.tip.dicom.net.DicomConnectionProperties;
import org.nrg.tip.dicom.strategy.orm.OrmStrategy;

public class CFindSCUSeriesLevelByIdWithCMove extends CFindSCUSeriesLevelById {

    public CFindSCUSeriesLevelByIdWithCMove(DicomConnectionProperties dicomConnectionProperties, CEchoSCU cechoSCU,
                                            OrmStrategy ormStrategy) {
        super(dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    @Override
    protected boolean cMoveRequestedOnResults() {
        return true;
    }

    @Override
    protected DcmQR createDcmQR(String localAETitle) {
        return new TipCMoveDcmQR(localAETitle);
    }
}
