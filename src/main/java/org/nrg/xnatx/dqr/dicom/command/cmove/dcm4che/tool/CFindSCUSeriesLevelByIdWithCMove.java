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
import org.nrg.xnatx.dqr.services.SeriesRetrievalRequestService;

public class CFindSCUSeriesLevelByIdWithCMove extends CFindSCUSeriesLevelById {
    private final String destinationProject;

    public CFindSCUSeriesLevelByIdWithCMove(final DqrPreferences preferences,
                                            final DicomConnectionProperties dicomConnectionProperties,
                                            final CEchoSCU cechoSCU,
                                            final OrmStrategy ormStrategy,
                                            final SeriesRetrievalRequestService seriesRetrievalRequestService,
                                            final String destinationProject
    ) {
        super(preferences, dicomConnectionProperties, cechoSCU, ormStrategy, seriesRetrievalRequestService);
        this.destinationProject = destinationProject;
    }

    @Override
    protected boolean cMoveRequestedOnResults() {
        return true;
    }

    @Override
    protected String getDestinationProject() {
        return destinationProject;
    }

    @Override
    protected DcmQR createDcmQR(final String localAETitle) {
        return new DqrCMoveDcmQR(localAETitle);
    }
}
