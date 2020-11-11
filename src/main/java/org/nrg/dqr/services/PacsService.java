/*
 * org.nrg.dqr.services.PacsService
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

import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.dqr.domain.Patient;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.util.CsvRow;
import org.nrg.dqr.util.FindRow;
import org.nrg.dqr.util.StudyImportInformation;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;
import org.nrg.xnat.restlet.extensions.PacsNotQueryableException;
import org.nrg.xnat.restlet.extensions.PacsNotStorableException;

import java.io.File;
import java.util.List;
import java.util.Map;

public interface PacsService {
    boolean canConnect(UserI user, final Pacs pacs);

    PacsSearchResults<String, Patient> getPatientsByExample(UserI user, final Pacs pacs, final PacsSearchCriteria searchCriteria);

    Patient getPatientById(UserI user, final Pacs pacs, String patientId);

    PacsSearchResults<String, Study> getStudiesByExample(UserI user, final Pacs pacs, final PacsSearchCriteria searchCriteria);

    Study getStudyById(UserI user, final Pacs pacs, final String studyInstanceUid);

    PacsSearchResults<String, Series> getSeriesByStudy(UserI user, final Pacs pacs, final Study study);

    @SuppressWarnings("unused")
    Series getSeriesById(UserI user, final Pacs pacs, final String seriesInstanceUid);

    PacsSearchResults<String, Series> getSeriesByStudyUid(final UserI user, final Pacs pacs, final String studyUid);

    void importSeries(UserI user, final Pacs pacs, final Study study, final Series series, final String ae);

    void importFromPacsRequest(final ExecutedPacsRequest request) throws PacsNotQueryableException, PacsNotStorableException;

    void exportSeries(UserI user, final Pacs pacs, final XnatImagescandata series);

    boolean aeIsStorable(final String ae);

    boolean processSpreadsheetImport(Map<String, StudyImportInformation> studiesToImport, UserI user, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception;

    boolean processSpreadsheetImportFromRows(UserI user, List<CsvRow> rows, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception;

    void processSpreadsheetImport(UserI user, File csv, String ae, String project, long pacsId) throws PacsNotFoundException, ConfigServiceException;

    List<CsvRow> extractImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) throws Exception;

    List<FindRow> extractNewImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) throws Exception;
}
