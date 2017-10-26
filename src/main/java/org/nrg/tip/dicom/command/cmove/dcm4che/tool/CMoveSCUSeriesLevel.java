/*
 * org.nrg.tip.dicom.command.cmove.dcm4che.tool.CMoveSCUSeriesLevel
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

import org.nrg.tip.dicom.command.cecho.CEchoSCU;
import org.nrg.tip.dicom.net.DicomConnectionProperties;
import org.nrg.tip.dicom.strategy.orm.OrmStrategy;
import org.nrg.tip.dto.PacsSearchCriteria;

public class CMoveSCUSeriesLevel {

    private CFindSCUSeriesLevelByIdWithCMove cfindSCU;

    public CMoveSCUSeriesLevel(DicomConnectionProperties dicomConnectionProperties, CEchoSCU cechoSCU,
                               OrmStrategy ormStrategy) {
        cfindSCU = new CFindSCUSeriesLevelByIdWithCMove(dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    public void cmove(PacsSearchCriteria searchCriteria) {
        cfindSCU.cfind(searchCriteria);
    }
}
