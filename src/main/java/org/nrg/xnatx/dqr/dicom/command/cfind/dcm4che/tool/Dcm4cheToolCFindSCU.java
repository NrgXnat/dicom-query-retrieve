/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.Dcm4cheToolCFindSCU
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool;

import org.nrg.xft.security.UserI;
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
import org.nrg.xnatx.dqr.services.SeriesRetrievalRequestService;
import org.nrg.xnatx.dqr.utils.OptionalString;

import java.util.Optional;

public class Dcm4cheToolCFindSCU implements CFindSCU {
    public Dcm4cheToolCFindSCU(final DqrPreferences preferences,
                               final UserI user,
                               final DicomConnectionProperties dicomConnectionProperties,
                               final OrmStrategy ormStrategy,
                               final SeriesRetrievalRequestService seriesRetrievalRequestService) {
        _preferences = preferences;
        _user = user;
        _dicomConnectionProperties = dicomConnectionProperties;
        _cechoSCU = new Dcm4cheToolCEchoSCU(preferences, dicomConnectionProperties);
        _ormStrategy = ormStrategy;
        _seriesRetrievalRequestService = seriesRetrievalRequestService;
    }

    @Override
    public PacsSearchResults<Patient> cfindPatientsByExample(final PacsSearchCriteria searchCriteria) {
        return new CFindSCUPatientLevelByExample(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy, _seriesRetrievalRequestService)
                .cfind(_user, searchCriteria);
    }

    @Override
    public Optional<Patient> cfindPatientById(final String patientId) {
        return OptionalString.of(patientId)
                .flatMap(pid ->
                        new CFindSCUPatientLevelById(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy, _seriesRetrievalRequestService)
                                .cfind(_user, PacsSearchCriteria.builder().patientId(pid).build())
                                .getFirstResult());
    }

    @Override
    public PacsSearchResults<Study> cfindStudiesByExample(final PacsSearchCriteria searchCriteria) {
        return new CFindSCUStudyLevelByExample(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy, _seriesRetrievalRequestService)
                .cfind(_user, searchCriteria);
    }

    @Override
    public Optional<Study> cfindStudyById(final String studyInstanceUid) {
        return OptionalString.of(studyInstanceUid)
                .flatMap(uid ->
                        new CFindSCUStudyLevelById(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy, _seriesRetrievalRequestService)
                                .cfind(_user, PacsSearchCriteria.builder().studyInstanceUid(uid).build())
                                .getFirstResult());
    }

    @Override
    public PacsSearchResults<Series> cfindSeriesByStudy(final Study study) {
        return study == null ? PacsSearchResults.emptyResults() : cfindSeriesByStudyUid(study.getStudyInstanceUid());
    }

    @Override
    public PacsSearchResults<Series> cfindSeriesByStudyUid(final String studyUid) {
        return OptionalString.of(studyUid)
                .map(uid ->
                        new CFindSCUSeriesLevelByStudy(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy, _seriesRetrievalRequestService)
                                .cfind(_user, PacsSearchCriteria.builder().studyInstanceUid(uid).build()))
                .orElse(PacsSearchResults.emptyResults());
    }

    @Override
    public Optional<Series> cfindSeriesById(final String seriesInstanceUid) {
        return OptionalString.of(seriesInstanceUid)
                .flatMap(uid ->
                        new CFindSCUSeriesLevelById(_preferences, _dicomConnectionProperties, _cechoSCU, _ormStrategy, _seriesRetrievalRequestService)
                                .cfind(_user, PacsSearchCriteria.builder().seriesInstanceUid(uid).build())
                                .getFirstResult());
    }

    /**
     * Post-construction get made available for unit testing hackage.
     */
    public OrmStrategy getOrmStrategy() {
        return _ormStrategy;
    }

    private final DicomConnectionProperties    _dicomConnectionProperties;
    private final UserI                        _user;
    private final CEchoSCU                     _cechoSCU;
    private final OrmStrategy                  _ormStrategy;
    private final DqrPreferences               _preferences;
    private final SeriesRetrievalRequestService _seriesRetrievalRequestService;
}
