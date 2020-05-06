/*
 * org.nrg.xnatx.dqr.services.impl.mock.MockPacsService
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.services.impl.mock;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.PatientName;
import org.nrg.xnatx.dqr.domain.ReferringPhysicianName;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.utils.CsvRow;
import org.nrg.xnatx.dqr.utils.FindRow;
import org.nrg.xnatx.dqr.utils.StudyImportInformation;

@SuppressWarnings("unused")
public class MockPacsService implements PacsService {
    @Override
    public boolean canConnect(UserI user, final Pacs pacs) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public PacsSearchResults<String, Patient> getPatientsByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria searchCriteria) {
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
    public PacsSearchResults<String, Study> getStudiesByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria searchCriteria) {
        final Patient            patient = getMockPatient();
        final Map<String, Study> studies = new HashMap<>(2);
        studies.put("34525253.34235.23456.1", getMockStudy(patient, "34525253.34235.23456.1"));
        studies.put("65860386.24536543.25922", getMockStudy(patient, "65860386.24536543.25922"));
        return PacsSearchResults.<String, Study>builder().results(studies).hasLimitedResultSetSize(true).build();
    }

    private Patient getMockPatient() {
        final Patient patient = new Patient();
        patient.setId("8675309");
        patient.setName(new PatientName("Jenny, Jenny"));
        try {
            patient.setBirthDate(DATE_FORMAT.parse("1990-01-01"));
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
        patient.setSex("F");
        return patient;
    }

    private Study getMockStudy(final Patient p, final String studyInstanceUid) {
        final Study study = new Study();
        study.setStudyInstanceUid(studyInstanceUid);
        study.setReferringPhysicianName(new ReferringPhysicianName("Tommy", "Tutone"));
        try {
            study.setStudyDate(DATE_FORMAT.parse("2008-01-01"));
        } catch (final ParseException e) {
            throw new RuntimeException(e);
        }
        study.setPatient(p);
        return study;
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
    public boolean processSpreadsheetImport(Map<String, StudyImportInformation> studiesToImport, UserI user, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean processSpreadsheetImportFromRows(UserI user, List<CsvRow> rows, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void processSpreadsheetImport(UserI user, File csv, String ae, String project, long pacsId) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public List<CsvRow> extractImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public List<FindRow> extractNewImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) {
        throw new RuntimeException("method not implemented");
    }

    private static Date getDate(final String date) {
        try {
            return DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            return new Date();
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
}
