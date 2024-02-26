/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.DicomQueryRetrieveService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import org.dcm4che3.data.Attributes;
import org.nrg.dcm.scp.exceptions.UnknownDicomScpInstanceException;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xapi.exceptions.InitializationException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.security.user.exceptions.UserNotFoundException;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.dto.PacsImportRequest;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.*;
import org.nrg.xnatx.dqr.utils.CsvRow;
import org.nrg.xnatx.dqr.utils.FindRow;

import java.io.File;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

@SuppressWarnings("unused")
public interface DicomQueryRetrieveService {
    /**
     * Indicates whether the specified user can connect to the indicated PACS system.
     *
     * @param user The user wanting to connect.
     * @param pacs The PACS to which the user wants to connect.
     *
     * @return Returns <b>true</b> if the user can connect to the specified PACS, <b>false</b> otherwise.
     */
    boolean ping(UserI user, Pacs pacs);

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
    PacsSearchResults<Patient> getPatientsByExample(UserI user, Pacs pacs, PacsSearchCriteria criteria) throws PacsException;

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
    PacsSearchResults<Study> getStudiesByExample(UserI user, Pacs pacs, PacsSearchCriteria criteria) throws PacsException, DataFormatException;

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
    Optional<Study> getStudyById(UserI user, Pacs pacs, String studyInstanceUid) throws PacsException;

    /**
     * Finds all study instance UIDs on the indicated PACS that match the specified keys.
     *
     * @param pacs The PACS to be searched
     * @param keys The criteria for filtering studies
     *
     * @return A list of study instance UIDs matching the specified keys.
     *
     * @throws PacsException When an error occurs connecting to the PACS
     */
    List<String> findStudyInstanceUids(Pacs pacs, Map<Integer, String> keys) throws PacsException;

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
    PacsSearchResults<Series> getSeriesByStudyUid(UserI user, Pacs pacs, String studyUid) throws PacsException;

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
    Map<String, PacsSearchResults<Series>> getSeriesByStudyUid(UserI user, Pacs pacs, List<String> studyUids) throws PacsException;

    /**
     * Get instance metadata from the PACS.
     *
     * @param pacs              The PACS to query.
     * @param studyInstanceUid  Study instance UID
     * @param seriesInstanceUid Series instance UID
     * @param sopInstanceUid    SOP instance UID
     * @param searchKeys
     * @return Returns instance attributes
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    Attributes getInstanceMetadata(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, Map<Integer, String> searchKeys) throws PacsException;

    /**
     * Finds all series instance UIDs on the indicated PACS that match the specified study instance UID.
     *
     * @param pacs The PACS to be searched
     * @param studyInstanceUid The study instance UID on which to filter the series
     *
     * @return A list of series instance UIDs matching the specified study instance UID.
     *
     * @throws PacsException When an error occurs connecting to the PACS
     */
    List<String> findSeriesInstanceUids(Pacs pacs, String studyInstanceUid) throws PacsException;

    /**
     * Finds all series instance UIDs on the indicated PACS that match the specified study instance UID.
     *
     * @param pacs The PACS to be searched
     * @param studyInstanceUid The study instance UID on which to filter the instances
     * @param seriesInstanceUid The series instance UID on which to filter the instances
     *
     * @return A list of series instance UIDs matching the specified study instance UID.
     *
     * @throws PacsException When an error occurs connecting to the PACS
     */
    List<String> findSopInstanceUids(Pacs pacs, String studyInstanceUid, String seriesInstanceUid) throws PacsException;

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
    void importSeries(UserI user, Pacs pacs, Study study, Series series, String ae) throws DqrException;

    /**
     * Import a single instance from the specified series
     *
     * @param pacs              The PACS to be searched
     * @param studyInstanceUid  The study instance UID on which to filter the series
     * @param seriesInstanceUid The series instance UID on which to filter the DICOM instances
     * @param sopInstanceUid    The instance to import
     * @param destinationAe     The AE title the PACS should use when sending the instance back to XNAT.
     * @throws PacsException When an error occurs connecting to the PACS
     */
    void importInstance(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, String destinationAe) throws PacsException;

    /**
     * Import data found in the {@link ExecutedPacsRequest completed PACS request} to this XNAT instance.
     *
     * @param request The completed request from which data should be imported.
     * @param user The XNAT user making the request
     */
    void importFromPacsRequest(ExecutedPacsRequest request, UserI user) throws DqrException;

    /**
     * Export the indicated {@link XnatImagescandata scans} from {@link XnatImagesessiondata session} to the specified PACS.
     *
     * @param user    The The user requesting the export operation.
     * @param pacs    The PACS to which the user wants to export.
     * @param session The session to be exported to the PACS.
     * @param scanIds The IDs of the scans from the session to be exported to the PACS.
     *
     * @return The workflow ID associated with the export operation.
     */
    Integer exportSession(UserI user, Pacs pacs, final XnatImagesessiondata session, final List<String> scanIds) throws InitializationException;

    /**
     * Export the indicated {@link XnatImagescandata series} to the specified PACS.
     *
     * @param user   The The user requesting the export operation.
     * @param pacs   The PACS to which the user wants to export.
     * @param series The series to be exported to the PACS.
     *
     * @return The boolean result of the operation
     */
    boolean exportSeries(UserI user, Pacs pacs, XnatImagescandata series);

    /**
     * Processes CSV import operations.
     *
     * @param user    The user requesting the import operation.
     * @param request Attributes for the import operation.
     *
     * @return Returns <b>true</b> if all data was imported and <b>false</b> if more data needs to be imported from the PACS.
     *
     * @throws ArchiveProcessorsNotAvailableException         When archive processors aren't available for the DICOM receiver.
     * @throws DicomReceiverCustomProcessingDisabledException When custom processing is disabled for the DICOM receiver.
     * @throws NotFoundException                              When the requested data can't be found.
     * @throws PacsNotFoundException                          When the specified PACS can't be found.
     * @throws PacsNotQueryableException                      When the specified PACS isn't queryable..
     * @throws UnknownDicomScpInstanceException               When the specified DICOM receiver doesn't exist.
     */
    List<QueuedPacsRequest> importFromPacs(final UserI user, final PacsImportRequest request) throws PacsNotFoundException, DicomReceiverCustomProcessingDisabledException, UnknownDicomScpInstanceException, NotFoundException, ArchiveProcessorsNotAvailableException, PacsNotQueryableException;

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
