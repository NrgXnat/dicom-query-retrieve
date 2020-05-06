/*
 * Dcm4cheToolCFindSCU
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import org.apache.commons.lang3.StringUtils;
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.CFindSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;

public class Dcm4cheToolCFindSCU implements CFindSCU {
    public Dcm4cheToolCFindSCU(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties, final OrmStrategy ormStrategy) {
        _preferences = preferences;
        _dicomConnectionProperties = dicomConnectionProperties;
        _cechoSCU = new Dcm4cheToolCEchoSCU(preferences, dicomConnectionProperties);
        _ormStrategy = ormStrategy;
    }

    @Override
    public PacsSearchResults<String, Patient> cfindPatientsByExample(final PacsSearchCriteria searchCriteria) {
        return new CFindSCUPatientLevelByExample(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(searchCriteria);
    }

    @Override
    public Patient cfindPatientById(final String patientId) {
        return new CFindSCUPatientLevelById(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(PacsSearchCriteria.builder().patientId(patientId).build()).getFirstResult();
    }

    @Override
    public PacsSearchResults<String, Study> cfindStudiesByExample(final PacsSearchCriteria searchCriteria) {
        return new CFindSCUStudyLevelByExample(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(searchCriteria);
    }

    @Override
    public Study cfindStudyById(final String studyInstanceUid) {
        return new CFindSCUStudyLevelById(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(PacsSearchCriteria.builder().studyInstanceUid(studyInstanceUid).build()).getFirstResult();
    }

    @Override
    public PacsSearchResults<String, Series> cfindSeriesByStudy(final Study study) {
        if (study == null) {
            return PacsSearchResults.emptyResults();
        }
        return cfindSeriesByStudyUid(study.getStudyInstanceUid());
    }

    @Override
    public PacsSearchResults<String, Series> cfindSeriesByStudyUid(final String studyUid) {
        if (StringUtils.isBlank(studyUid)) {
            return PacsSearchResults.emptyResults();
        }
        return new CFindSCUSeriesLevelByStudy(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(PacsSearchCriteria.builder().studyInstanceUid(studyUid).build());
    }

    @Override
    public Series cfindSeriesById(final String seriesInstanceUid) {
        return StringUtils.isBlank(seriesInstanceUid) ? null : new CFindSCUSeriesLevelById(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(PacsSearchCriteria.builder().seriesInstanceUid(seriesInstanceUid).build()).getFirstResult();
    }

    /**
     * Post-construction get made available for unit testing hackage.
     */
    public OrmStrategy getOrmStrategy() {
        return _ormStrategy;
    }

    private final DicomConnectionProperties _dicomConnectionProperties;
    private final CEchoSCU                  _cechoSCU;
    private final OrmStrategy               _ormStrategy;
    private final DqrPreferences            _preferences;
}
