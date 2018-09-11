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

package org.nrg.dqr.dicom.command.cfind.dcm4che.tool;

import org.nrg.dqr.dicom.command.cfind.CFindSCU;
import org.nrg.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.dqr.domain.Patient;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.dqr.preferences.DqrPreferences;

public class Dcm4cheToolCFindSCU implements CFindSCU {

    private DicomConnectionProperties dicomConnectionProperties;

    private CEchoSCU cechoSCU;

    private OrmStrategy ormStrategy;

    private final DqrPreferences _preferences;

    public Dcm4cheToolCFindSCU(final DqrPreferences preferences, final DicomConnectionProperties dicomConnectionProperties, final OrmStrategy ormStrategy) {
        _preferences = preferences;
        this.dicomConnectionProperties = dicomConnectionProperties;
        cechoSCU = new Dcm4cheToolCEchoSCU(preferences, dicomConnectionProperties);
        this.ormStrategy = ormStrategy;
    }

    @Override
    public PacsSearchResults<String, Patient> cfindPatientsByExample(final PacsSearchCriteria searchCriteria) {
        return new CFindSCUPatientLevelByExample(_preferences, dicomConnectionProperties, cechoSCU, ormStrategy)
                .cfind(searchCriteria);
    }

    @Override
    public Patient cfindPatientById(final String patientId) {
        PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
        searchCriteria.setPatientId(patientId);
        PacsSearchResults<String, Patient> searchResults = new CFindSCUPatientLevelById(_preferences, dicomConnectionProperties,
                cechoSCU, ormStrategy).cfind(searchCriteria);
        return searchResults.getFirstResult();
    }

    @Override
    public PacsSearchResults<String, Study> cfindStudiesByExample(final PacsSearchCriteria searchCriteria) {
        return new CFindSCUStudyLevelByExample(_preferences, dicomConnectionProperties, cechoSCU, ormStrategy).cfind(searchCriteria);
    }

    @Override
    public Study cfindStudyById(final String studyInstanceUid) {
        PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
        searchCriteria.setStudyInstanceUid(studyInstanceUid);
        PacsSearchResults<String, Study> searchResults = new CFindSCUStudyLevelById(_preferences, dicomConnectionProperties,
                cechoSCU, ormStrategy).cfind(searchCriteria);
        return searchResults.getFirstResult();
    }

    @Override
    public PacsSearchResults<String, Series> cfindSeriesByStudy(final Study study) {
        PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
        if (null != study) {
            searchCriteria.setStudyInstanceUid(study.getStudyInstanceUid());
        }
        return new CFindSCUSeriesLevelByStudy(_preferences, dicomConnectionProperties, cechoSCU, ormStrategy).cfind(searchCriteria);
    }

    @Override
    public Series cfindSeriesById(final String seriesInstanceUid) {
        PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
        searchCriteria.setSeriesInstanceUid(seriesInstanceUid);
        PacsSearchResults<String, Series> searchResults = new CFindSCUSeriesLevelById(_preferences, dicomConnectionProperties,
                cechoSCU, ormStrategy).cfind(searchCriteria);
        return searchResults.getFirstResult();
    }

    /**
     * Post-construction get made available for unit testing hackage.
     */
    public OrmStrategy getOrmStrategy() {
        return ormStrategy;
    }
}
