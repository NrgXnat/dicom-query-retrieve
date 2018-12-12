/*
 * org.nrg.dqr.services.MockPacsService
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.services;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.dqr.domain.Patient;
import org.nrg.dqr.domain.PatientName;
import org.nrg.dqr.domain.ReferringPhysicianName;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.util.CsvRow;
import org.nrg.dqr.util.SimpleCsvRow;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;

@SuppressWarnings("unused")
public class MockPacsService implements PacsService {

    @Override
    public boolean canConnect(UserI user, final Pacs pacs){
        throw new RuntimeException("method not implemented");
    }

    @Override
    public PacsSearchResults<String, Patient> getPatientsByExample(final UserI user, final Pacs pacs,
                                                                   final PacsSearchCriteria searchCriteria) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public Patient getPatientById(final UserI user, final Pacs pacs, final String patientId) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public Study getStudyById(final UserI user, final Pacs pacs, final String studyInstanceUid) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public PacsSearchResults<String, Study> getStudiesByExample(final UserI user, final Pacs pacs,
                                                                final PacsSearchCriteria searchCriteria) {
        final Patient patient = getMockPatient();
        final Map<String, Study> studies = new HashMap<String, Study>(2);
        studies.put("34525253.34235.23456.1", getMockStudy(patient, "34525253.34235.23456.1"));
        studies.put("65860386.24536543.25922", getMockStudy(patient, "65860386.24536543.25922"));
        return new PacsSearchResults<String, Study>(studies, true, null);
    }

    private Patient getMockPatient() {
        final Patient p = new Patient();
        p.setId("8675309");
        p.setName(new PatientName("Jenny, Jenny"));
        try {
            p.setBirthDate(new SimpleDateFormat("yyyy-MM-dd").parse("1990-01-01"));
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
        p.setSex("F");
        return p;
    }

    private Study getMockStudy(final Patient p, final String studyInstanceUid) {
        final Study s = new Study();
        s.setStudyInstanceUid(studyInstanceUid);
        s.setReferringPhysicianName(new ReferringPhysicianName("Tommy", "Tutone"));
        try {
            s.setStudyDate(new SimpleDateFormat("yyyy-MM-dd").parse("2008-01-01"));
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
        s.setPatient(p);
        return s;
    }

    @Override
    public PacsSearchResults<String, Series> getSeriesByStudy(final UserI user, final Pacs pacs, final Study study) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public PacsSearchResults<String, Series> getSeriesByStudyUid(final UserI user, final Pacs pacs, final String studyUid) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public Series getSeriesById(final UserI user, final Pacs pacs, final String seriesInstanceUid) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void importSeries(final UserI user, final Pacs pacs, final Study study, final Series series, final String ae) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void importFromPacsRequest(final ExecutedPacsRequest request) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void exportSeries(final UserI user, final Pacs pacs, final XnatImagescandata series) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean aeIsStorable(final String ae) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean processSpreadsheetImportFromRows(UserI user, List<CsvRow> rows, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws PacsNotFoundException, ConfigServiceException {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean processSpreadsheetImportFromSimpleRows(UserI user, List<SimpleCsvRow> rows, String ae, String project, long pacsId, String seriesDescriptions, boolean importEvenIfCustomProcessingIsOff) throws PacsNotFoundException, ConfigServiceException {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void processSpreadsheetImport(UserI user, File csv, String ae, String project, long pacsId) throws PacsNotFoundException {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public List<CsvRow> extractImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        throw new RuntimeException("method not implemented");
    }
}
