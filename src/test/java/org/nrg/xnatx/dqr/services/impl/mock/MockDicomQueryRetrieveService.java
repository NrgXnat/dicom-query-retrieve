/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.mock.MockDicomQueryRetrieveService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.mock;

import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.*;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.dto.PacsImportRequest;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.messaging.PacsSearchRequest;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;
import org.nrg.xnatx.dqr.utils.CsvRow;
import org.nrg.xnatx.dqr.utils.FindRow;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

@SuppressWarnings("unused")
public class MockDicomQueryRetrieveService implements DicomQueryRetrieveService {
    @Override
    public boolean canConnect(UserI user, final Pacs pacs) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public PacsSearchResults<Patient> getPatientsByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria criteria) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public Optional<Patient> getPatientById(final UserI user, final Pacs pacs, final String patientId) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public Optional<Study> getStudyById(final UserI user, final Pacs pacs, final String studyInstanceUid) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public PacsSearchResults<Study> getStudiesByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria criteria) {
        final Patient patient = getMockPatient();
        return PacsSearchResults.<Study>builder().results(Arrays.asList(getMockStudy(patient, "34525253.34235.23456.1"), getMockStudy(patient, "65860386.24536543.25922"))).hasLimitedResultSetSize(true).build();
    }

    @Override
    public PacsSearchResults<Series> getSeriesByStudy(final UserI user, final Pacs pacs, final Study study) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public PacsSearchResults<Series> getSeriesByStudyUid(final UserI user, final Pacs pacs, final String studyUid) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public Map<String, PacsSearchResults<Series>> getSeriesByStudyUid(final UserI user, final Pacs pacs, final List<String> studyUids) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean getSearchStatus(final UUID requestId) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public PacsSearchRequest getSearchRequest(final UUID requestId) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void updateSearchResults(final UUID requestId, final String studyInstanceUid, final PacsSearchResults<Series> results) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public UUID getSeriesByStudyUidAsync(final UserI user, final Pacs pacs, final List<String> studyUids) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public Map<String, PacsSearchResults<Series>> getSearchResults(final UUID requestId) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public Optional<Series> getSeriesById(final UserI user, final Pacs pacs, final String seriesInstanceUid) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void importSeries(final DimseRequestContext context, final Pacs pacs, final Study study, final Series series, final String ae) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void importFromPacsRequest(final UserI user, final ExecutedPacsRequest request) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public Integer exportSession(UserI user, Pacs pacs, XnatImagesessiondata session, List<String> scanIds) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean exportSeries(final UserI user, final Pacs pacs, final XnatImagescandata series) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean isAeStorable(final String ae) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public List<QueuedPacsRequest> importFromPacs(final UserI user, final String userDefinedId, final PacsImportRequest request) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public boolean processSpreadsheetImportFromRows(UserI user, String userDefinedId, List<CsvRow> rows, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) {
        throw new RuntimeException("method not implemented");
    }

    @Override
    public void processSpreadsheetImport(UserI user, String userDefinedId, File csv, String ae, String project, long pacsId) {
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

    private Patient getMockPatient() {
        final Patient patient = new Patient();
        patient.setId("8675309");
        patient.setName(new DqrPersonName("Jenny, Jenny"));
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

    private static Date getDate(final String date) {
        try {
            return DATE_FORMAT.parse(date);
        } catch (ParseException e) {
            return new Date();
        }
    }

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd");
}
