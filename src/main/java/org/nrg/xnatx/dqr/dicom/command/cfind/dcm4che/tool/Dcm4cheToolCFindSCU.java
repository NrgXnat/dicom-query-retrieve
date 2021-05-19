/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.Dcm4cheToolCFindSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
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

import java.util.Optional;

public class Dcm4cheToolCFindSCU implements CFindSCU {
    public Dcm4cheToolCFindSCU(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties, final OrmStrategy ormStrategy) {
        _preferences = preferences;
        _dicomConnectionProperties = dicomConnectionProperties;
        _cechoSCU = new Dcm4cheToolCEchoSCU(preferences, dicomConnectionProperties);
        _ormStrategy = ormStrategy;
    }

    @Override
    public PacsSearchResults<Patient> cfindPatientsByExample(final PacsSearchCriteria searchCriteria) {
        return new CFindSCUPatientLevelByExample(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(searchCriteria);
    }

    @Override
    public Optional<Patient> cfindPatientById(final String patientId) {
        return StringUtils.isBlank(patientId) ? Optional.empty() : new CFindSCUPatientLevelById(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(PacsSearchCriteria.builder().patientId(patientId).build()).getFirstResult();
    }

    @Override
    public PacsSearchResults<Study> cfindStudiesByExample(final PacsSearchCriteria searchCriteria) {
        return new CFindSCUStudyLevelByExample(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(searchCriteria);
    }

    @Override
    public Optional<Study> cfindStudyById(final String studyInstanceUid) {
        return StringUtils.isBlank(studyInstanceUid) ? Optional.empty() : new CFindSCUStudyLevelById(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(PacsSearchCriteria.builder().studyInstanceUid(studyInstanceUid).build()).getFirstResult();
    }

    @Override
    public PacsSearchResults<Series> cfindSeriesByStudy(final Study study) {
        return study == null ? PacsSearchResults.emptyResults() : cfindSeriesByStudyUid(study.getStudyInstanceUid());
    }

    @Override
    public PacsSearchResults<Series> cfindSeriesByStudyUid(final String studyUid) {
        return StringUtils.isBlank(studyUid) ? PacsSearchResults.emptyResults() : new CFindSCUSeriesLevelByStudy(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(PacsSearchCriteria.builder().studyInstanceUid(studyUid).build());
    }

    @Override
    public Optional<Series> cfindSeriesById(final String seriesInstanceUid) {
        return StringUtils.isBlank(seriesInstanceUid) ? Optional.empty() : new CFindSCUSeriesLevelById(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy).cfind(PacsSearchCriteria.builder().seriesInstanceUid(seriesInstanceUid).build()).getFirstResult();
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
