/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.Dcm4cheToolCMoveSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool;

import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.DimseRequestContext;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

public class Dcm4cheToolCMoveSCU implements CMoveSCU {
    public Dcm4cheToolCMoveSCU(final DqrPreferences preferences,
                               final DicomConnectionProperties dicomConnectionProperties,
                               final OrmStrategy ormStrategy) {
        _dicomConnectionProperties = dicomConnectionProperties;
        _cEchoSCU = new Dcm4cheToolCEchoSCU(preferences, dicomConnectionProperties);
        _ormStrategy = ormStrategy;
        _preferences = preferences;
    }

    @Override
    public void cmoveSeries(final DimseRequestContext context, final Study study, final Series series) {
        final PacsSearchCriteria.PacsSearchCriteriaBuilder builder = PacsSearchCriteria.builder();
        if (study != null) {
            builder.studyInstanceUid(study.getStudyInstanceUid());
        }
        if (series != null) {
            builder.seriesInstanceUid(series.getSeriesInstanceUid());
        }
        new CMoveSCUSeriesLevel(_preferences, _dicomConnectionProperties, _cEchoSCU, _ormStrategy, study.getProjectId())
                .cmove(context, builder.build());
    }

    private final DicomConnectionProperties _dicomConnectionProperties;
    private final CEchoSCU                  _cEchoSCU;
    private final OrmStrategy               _ormStrategy;
    private final DqrPreferences            _preferences;
}
