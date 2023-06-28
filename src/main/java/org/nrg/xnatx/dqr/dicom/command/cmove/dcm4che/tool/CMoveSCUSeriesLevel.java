/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.CMoveSCUSeriesLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool;

import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.DimseRequestContext;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

public class CMoveSCUSeriesLevel {
    private final CFindSCUSeriesLevelByIdWithCMove cfindSCU;

    public CMoveSCUSeriesLevel(final DqrPreferences preferences,
                               final DicomConnectionProperties dicomConnectionProperties,
                               final CEchoSCU cechoSCU,
                               final OrmStrategy ormStrategy,
                               final String destinationProject) {
        cfindSCU = new CFindSCUSeriesLevelByIdWithCMove(preferences, dicomConnectionProperties, cechoSCU, ormStrategy, destinationProject);
    }

    public void cmove(final DimseRequestContext context, final PacsSearchCriteria searchCriteria) {
        cfindSCU.cfind(context, searchCriteria);
    }
}
