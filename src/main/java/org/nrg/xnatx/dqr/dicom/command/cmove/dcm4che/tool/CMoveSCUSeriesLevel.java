/*
 * CMoveSCUSeriesLevel
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool;

import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

public class CMoveSCUSeriesLevel {

    private CFindSCUSeriesLevelByIdWithCMove cfindSCU;

    public CMoveSCUSeriesLevel(final DqrPreferences preferences, DicomConnectionProperties dicomConnectionProperties, CEchoSCU cechoSCU,
                               OrmStrategy ormStrategy) {
        cfindSCU = new CFindSCUSeriesLevelByIdWithCMove(preferences, dicomConnectionProperties, cechoSCU, ormStrategy);
    }

    public void cmove(PacsSearchCriteria searchCriteria) {
        cfindSCU.cfind(searchCriteria);
    }
}
