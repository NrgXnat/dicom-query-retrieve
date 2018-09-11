/*
 * Dcm4cheToolCMoveSCU
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

import org.nrg.dqr.domain.Study;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.dqr.dicom.command.cmove.CMoveSCU;
import org.nrg.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.preferences.DqrPreferences;

public class Dcm4cheToolCMoveSCU implements CMoveSCU {

    private DicomConnectionProperties dicomConnectionProperties;

    private CEchoSCU cechoSCU;

    private OrmStrategy ormStrategy;

    private final DqrPreferences _preferences;

    public Dcm4cheToolCMoveSCU(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties, final OrmStrategy ormStrategy) {
        this.dicomConnectionProperties = dicomConnectionProperties;
        cechoSCU = new Dcm4cheToolCEchoSCU(preferences, dicomConnectionProperties);
        this.ormStrategy = ormStrategy;
        _preferences = preferences;
    }

    @Override
    public void cmoveSeries(Study study, Series series) {
        PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
        if (study != null) {
            searchCriteria.setStudyInstanceUid(study.getStudyInstanceUid());
        }
        if (series != null) {
            searchCriteria.setSeriesInstanceUid(series.getSeriesInstanceUid());
        }
        new CMoveSCUSeriesLevel(_preferences, dicomConnectionProperties, cechoSCU, ormStrategy).cmove(searchCriteria);
    }
}
