/*
 * org.nrg.tip.dicom.command.cmove.dcm4che.tool.Dcm4cheToolCMoveSCU
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
import org.nrg.tip.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.tip.dicom.command.cmove.CMoveSCU;
import org.nrg.tip.dicom.net.DicomConnectionProperties;
import org.nrg.tip.dicom.strategy.orm.OrmStrategy;
import org.nrg.tip.domain.Series;
import org.nrg.tip.domain.Study;
import org.nrg.tip.dto.PacsSearchCriteria;

public class Dcm4cheToolCMoveSCU implements CMoveSCU {

    private DicomConnectionProperties dicomConnectionProperties;

    private CEchoSCU cechoSCU;

    private OrmStrategy ormStrategy;

    public Dcm4cheToolCMoveSCU(final DicomConnectionProperties dicomConnectionProperties, final OrmStrategy ormStrategy) {
        this.dicomConnectionProperties = dicomConnectionProperties;
        cechoSCU = new Dcm4cheToolCEchoSCU(dicomConnectionProperties);
        this.ormStrategy = ormStrategy;
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
        new CMoveSCUSeriesLevel(dicomConnectionProperties, cechoSCU, ormStrategy).cmove(searchCriteria);
    }
}
