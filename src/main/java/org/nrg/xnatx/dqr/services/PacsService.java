/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.PacsService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;
import org.nrg.xnatx.dqr.messaging.PacsSearchRequest;
import org.nrg.xnatx.dqr.utils.CsvRow;
import org.nrg.xnatx.dqr.utils.FindRow;
import org.nrg.xnatx.dqr.utils.StudyImportInformation;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public interface PacsService {
    /**
     * Indicates whether the specified user can connect to the indicated PACS system.
     *
     * @param user The user wanting to connect.
     * @param pacs The PACS to which the user wants to connect.
     *
     * @return Returns <b>true</b> if the user can connect to the specified PACS, <b>false</b> otherwise.
     */
    boolean canConnect(UserI user, Pacs pacs);

    /**
     * Searches for patients on the specified PACS that match the given criteria.
     *
     * @param user     The user requesting the query.
     * @param pacs     The PACS to query.
     * @param criteria The criteria on which to search.
     *
     * @return Returns patients matching the specified criteria.
     *
     * @throws PacsNotQueryableException Thrown when the PACS can't be queried.
     */
    PacsSearchResults<Patient> getPatientsByExample(UserI user, Pacs pacs, PacsSearchCriteria criteria) throws PacsNotQueryableException;

    /**
     * Searches for a patient on the specified PACS with the indicated ID. This is a simplified version of the
     * {@link #getPatientsByExample(UserI, Pacs, PacsSearchCriteria)} method.
     *
     * @param user      The user requesting the query.
     * @param pacs      The PACS to query.
     * @param patientId The ID of the patient.
     *
     * @return The patient with the indicated ID if found.
     *
     * @throws PacsNotQueryableException Thrown when the PACS can't be queried.
     */
    Optional<Patient> getPatientById(UserI user, Pacs pacs, String patientId) throws PacsNotQueryableException;

    /**
     * Searches for studies on the specified PACS that match the given criteria.
     *
     * @param user     The user requesting the query.
     * @param pacs     The PACS to query.
     * @param criteria The criteria on which to search.
     *
     * @return Returns studies matching the specified criteria.
     *
     * @throws PacsNotQueryableException Thrown when the PACS can't be queried.
     */
    PacsSearchResults<Study> getStudiesByExample(UserI user, Pacs pacs, PacsSearchCriteria criteria) throws PacsNotQueryableException;

    /**
     * Searches for a study on the specified PACS with the indicated study instance UID. This is a simplified version of the
     * {@link #getStudiesByExample(UserI, Pacs, PacsSearchCriteria)} method.
     *
     * @param user             The user requesting the query.
     * @param pacs             The PACS to query.
     * @param studyInstanceUid The study instance ID.
     *
     * @return The study with the indicated study instance UID if found.
     *
     * @throws PacsNotQueryableException Thrown when the PACS can't be queried.
     */
    Optional<Study> getStudyById(UserI user, Pacs pacs, String studyInstanceUid) throws PacsNotQueryableException;

    /**
     * Searches for series from the submitted study on the specified PACS.
     *
     * @param user  The user requesting the query.
     * @param pacs  The PACS to query.
     * @param study The study on which to search.
     *
     * @return Returns series from the specified study if found.
     *
     * @throws PacsNotQueryableException Thrown when the PACS can't be queried.
     */
    PacsSearchResults<Series> getSeriesByStudy(UserI user, Pacs pacs, Study study) throws PacsNotQueryableException;

    /**
     * Searches for the series on the specified PACS with the indicated series instance UID.
     *
     * @param user              The user requesting the query.
     * @param pacs              The PACS to query.
     * @param seriesInstanceUid The series instance UID on which to search.
     *
     * @return Returns the specified series if found.
     *
     * @throws PacsNotQueryableException Thrown when the PACS can't be queried.
     */
    @SuppressWarnings("unused")
    Optional<Series> getSeriesById(UserI user, Pacs pacs, String seriesInstanceUid) throws PacsNotQueryableException;

    /**
     * Runs a synchronous query against the specified PACS to find a study with the indicated study instance UID.
     *
     * @param user     The user requesting the query.
     * @param pacs     The PACS to query.
     * @param studyUid The study instance UID to search on.
     *
     * @return Returns series from the requested study if found.
     *
     * @throws PacsNotQueryableException Thrown when the PACS can't be queried.
     */
    PacsSearchResults<Series> getSeriesByStudyUid(UserI user, Pacs pacs, String studyUid) throws PacsNotQueryableException;

    /**
     * Runs a synchronous query against the specified PACS to find studies with the indicated study instance UIDs.
     *
     * @param user      The user requesting the query.
     * @param pacs      The PACS to query.
     * @param studyUids The study instance UIDs to search on.
     *
     * @return Returns series from the requested studies if found, organized by study instance UID.
     *
     * @throws PacsNotQueryableException Thrown when the PACS can't be queried.
     */
    Map<String, PacsSearchResults<Series>> getSeriesByStudyUid(UserI user, Pacs pacs, List<String> studyUids) throws PacsNotQueryableException;

    /**
     * Starts an asynchronous query against the specified PACS to find studies with the indicated study instance UIDs.
     * Compare with {@link #getSeriesByStudyUid(UserI, Pacs, String)} and {@link #getSeriesByStudyUid(UserI, Pacs, List)}:
     * the core functionality is the same, but those synchronous calls require staying connected for the duration of
     * the query operation.
     *
     * @param user      The user requesting the query.
     * @param pacs      The PACS to query.
     * @param studyUids The study instance UIDs to search on.
     *
     * @return The UUID of the queued query.
     *
     * @throws PacsNotQueryableException Thrown when the PACS can't be queried.
     */
    UUID getSeriesByStudyUidAsync(UserI user, Pacs pacs, List<String> studyUids) throws PacsNotQueryableException;

    /**
     * Indicates whether the specified search request has completed.
     *
     * @param requestId The ID of the search request.
     *
     * @return Returns <b>true</b> if the search has completed, <b>false</b> otherwise.
     *
     * @throws NotFoundException Thrown when the specified search request ID is not found.
     */
    boolean getSearchStatus(UUID requestId) throws NotFoundException;

    /**
     * Returns the parameters for the specified search request.
     *
     * @param requestId The ID of the search request.
     *
     * @return The {@link PacsSearchRequest request object} for the specified search.
     *
     * @throws NotFoundException Thrown when the specified search request ID is not found.
     */
    PacsSearchRequest getSearchRequest(UUID requestId) throws NotFoundException;

    /**
     * Updates the specified search request, adding the submitted {@link PacsSearchResults results object} to the stored results.
     *
     * @param requestId        The ID of the search request.
     * @param studyInstanceUid The study instance UID of the result.
     * @param results          The results of the query for the study instance UID.
     *
     * @throws NotFoundException Thrown when the specified search request ID is not found.
     */
    void updateSearchResults(UUID requestId, String studyInstanceUid, PacsSearchResults<Series> results) throws NotFoundException;

    /**
     * Returns the results of the search with the specified search request ID. Note that this can be called at any point while
     * the search is being run, so the results may not be complete. Call {@link #getSearchStatus(UUID)} to determine if the
     * search has completed.
     *
     * @param requestId The ID of the search request.
     *
     * @return The results of the specified search request.
     *
     * @throws NotFoundException Thrown when the specified search request ID is not found.
     */
    Map<String, PacsSearchResults<Series>> getSearchResults(UUID requestId) throws NotFoundException;

    /**
     * Import the specified series from the indicated PACS to this XNAT instance.
     *
     * @param user   The The user requesting the import operation.
     * @param pacs   The PACS from which the user wants to import.
     * @param study  The study containing the desired series.
     * @param series The series to be imported.
     * @param ae     The AE title the PACS should use when sending the series back to XNAT.
     */
    void importSeries(UserI user, Pacs pacs, Study study, Series series, String ae);

    /**
     * Import data found in the {@link ExecutedPacsRequest completed PACS request} to this XNAT instance.
     *
     * @param request The completed request from which data should be imported.
     */
    void importFromPacsRequest(ExecutedPacsRequest request) throws PacsNotQueryableException, PacsNotStorableException;

    /**
     * Export the indicated {@link XnatImagescandata series} to the specified PACS.
     *
     * @param user   The The user requesting the export operation.
     * @param pacs   The PACS to which the user wants to export.
     * @param series The series to be exported to the PACS.
     */
    void exportSeries(UserI user, Pacs pacs, XnatImagescandata series);

    /**
     * Indicates whether the specified DICOM receiver supports C-STORE operations.
     *
     * @param ae The AE title and optional port of the DICOM receiver to check.
     *
     * @return Returns <b>true</b> if the receiver supports C-STORE operations and <b>false</b> otherwise.
     */
    boolean isAeStorable(String ae);

    /**
     * Processes CSV import operations.
     *
     * @param studiesToImport                   The studies to import.
     * @param user                              The user requesting the import operation.
     * @param ae                                The AE receiving the data to be imported.
     * @param project                           The project to which the data should be imported.
     * @param pacsId                            The PACS from which the data should be imported.
     * @param importEvenIfCustomProcessingIsOff Indicates that the data should be imported even if the AE doesn't currently support custom processing.
     *
     * @return Returns <b>true</b> if all data was imported and <b>false</b> if more data needs to be imported from the PACS.
     *
     * @throws Exception When an unexpected error occurs.
     */
    boolean processSpreadsheetImport(Map<String, StudyImportInformation> studiesToImport, UserI user, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception;

    /**
     * Processes CSV import operations.
     *
     * @param user                              The user requesting the import operation.
     * @param rows                              The rows to be imported.
     * @param ae                                The AE receiving the data to be imported.
     * @param project                           The project to which the data should be imported.
     * @param pacsId                            The PACS from which the data should be imported.
     * @param importEvenIfCustomProcessingIsOff Indicates that the data should be imported even if the AE doesn't currently support custom processing.
     *
     * @return Returns <b>true</b> if all data was imported and <b>false</b> if more data needs to be imported from the PACS.
     *
     * @throws Exception When an unexpected error occurs.
     */
    boolean processSpreadsheetImportFromRows(UserI user, List<CsvRow> rows, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception;

    /**
     * Processes CSV import operations.
     *
     * @param user    The user requesting the import operation.
     * @param csv     The CSV file to import.
     * @param ae      The AE receiving the data to be imported.
     * @param project The project to which the data should be imported.
     * @param pacsId  The PACS from which the data should be imported.
     *
     * @throws PacsNotFoundException When no PACS with the specified ID is known.
     */
    void processSpreadsheetImport(UserI user, File csv, String ae, String project, long pacsId) throws PacsNotFoundException;

    /**
     * Extracts rows from the CSV file.
     *
     * @param user                             The user requesting CSV processing.
     * @param csv                              The CSV file to import.
     * @param pacsId                           The PACS from which the data should be imported.
     * @param allowRowThatGetsAllStudiesOnPacs Allow row that retrieves all data from target PACS.
     *
     * @return A list of rows from the CSV file.
     *
     * @throws Exception When an unexpected error occurs.
     */
    List<CsvRow> extractImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) throws Exception;

    /**
     * Extracts import request rows from the CSV file.
     *
     * @param user                             The user requesting CSV processing.
     * @param csv                              The CSV file to import.
     * @param pacsId                           The PACS from which the data should be imported.
     * @param allowRowThatGetsAllStudiesOnPacs Allow row that retrieves all data from target PACS.
     *
     * @return A list of rows from the CSV file.
     *
     * @throws Exception When an unexpected error occurs.
     */
    List<FindRow> extractNewImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) throws Exception;
}
