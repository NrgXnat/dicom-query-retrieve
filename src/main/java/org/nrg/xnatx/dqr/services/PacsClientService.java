package org.nrg.xnatx.dqr.services;

import org.dcm4che3.data.Attributes;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.DqrException;
import org.nrg.xnatx.dqr.exceptions.PacsException;

import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;

public interface PacsClientService {
    /**
     * Indicates whether XNAT can connect to the indicated PACS system.
     *
     * @param pacs The PACS to which the user wants to connect.
     *
     * @return Returns <b>true</b> if XNAT can connect to the specified PACS, <b>false</b> otherwise.
     */
    boolean ping(Pacs pacs);

    /**
     * Searches for a study on the specified PACS with the indicated study instance UID.
     *
     * @param pacs             The PACS to query.
     * @param studyInstanceUid The study instance ID.
     *
     * @return The study with the indicated study instance UID if found.
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    Optional<Study> getStudy(Pacs pacs, String studyInstanceUid) throws PacsException;

    /**
     * Searches for patients on the specified PACS that match the given criteria.
     *
     * @param pacs The PACS to query.
     * @param searchCriteria The criteria on which to search.
     *
     * @return Returns patients matching the specified criteria.
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    PacsSearchResults<Patient> queryPatients(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException;

    /**
     * Searches for studies on the specified PACS that match the given criteria.
     *
     * @param pacs The PACS to query.
     * @param searchCriteria The criteria on which to search.
     *
     * @return Returns studies matching the specified criteria.
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    PacsSearchResults<Study> queryStudies(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException, DataFormatException;

    /**
     * Searches for series on the specified PACS that match the given criteria.
     *
     * @param pacs      The PACS to query.
     * @param searchCriteria The criteria on which to search.
     *
     * @return Returns studies matching the specified criteria.
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    PacsSearchResults<Series> querySeries(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException;

    /**
     * Execute a query on the specified PACS
     *
     * @param pacs      The PACS to query.
     * @param searchKeys The criteria on which to search.
     * @param callback  The callback method to execute on each dataset retrieved
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    void queryPatients(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException;

    /**
     * Execute a query on the specified PACS
     *
     * @param pacs      The PACS to query.
     * @param searchKeys The criteria on which to search.
     * @param callback  The callback method to execute on each dataset retrieved
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    void queryStudies(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException;

    /**
     * Execute a query on the specified PACS
     *
     * @param pacs      The PACS to query.
     * @param searchKeys The criteria on which to search.
     * @param callback  The callback method to execute on each dataset retrieved
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    void querySeries(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException;

    /**
     * Execute a query on the specified PACS
     *
     * @param pacs      The PACS to query.
     * @param searchKeys The criteria on which to search.
     * @param callback  The callback method to execute on each dataset retrieved
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    void querySeries(Pacs pacs, String studyInstanceUid, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException;

    /**
     * Execute a query on the specified PACS
     *
     * @param pacs      The PACS to query.
     * @param searchKeys The criteria on which to search.
     * @param callback  The callback method to execute on each dataset retrieved
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    void queryInstance(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException;

    /**
     * Execute a query on the specified PACS
     *
     * @param pacs      The PACS to query.
     * @param searchKeys The criteria on which to search.
     * @param callback  The callback method to execute on each dataset retrieved
     *
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    void queryInstance(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException;

    /**
     * Get instance metadata attributes from the PACS
     *
     * @param pacs              The PACS to query.
     * @param studyInstanceUid  Study instance UID
     * @param seriesInstanceUid Series instance UID
     * @param sopInstanceUid    SOP instance UID
     * @param searchKeys
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    Attributes getInstanceMetadata(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, Map<Integer, String> searchKeys) throws PacsException;

    /**
     * Get instance metadata from the PACS given its RetrieveURL
     *
     * @param pacs        The PACS to query.
     * @param retrieveUrl The URL to retrieve one of these objects from the PACS.
     *                    This is found from the RetrieveURL tag in the DICOM attributes of a query.
     *                    We will append /metadata to the URL to get the metadata.
     * @param searchKeys  The criteria on which to search.
     * @throws PacsException Thrown when the PACS can't be queried.
     */
    Attributes getInstanceMetadata(Pacs pacs, String retrieveUrl, Map<Integer, String> searchKeys) throws PacsException;

    /**
     * Import the specified series from the indicated PACS to this XNAT instance.
     *
     * @param pacs   The PACS from which the user wants to import.
     * @param user   XNAT user importing the series.
     * @param study  The study containing the desired series.
     * @param series The series to be imported.
     * @param ae     The AE title the PACS should use when sending the series back to XNAT.
     * @param user   The user that launched the requuest
     */
    void importSeries(Pacs pacs, UserI user, Study study, Series series, String ae) throws DqrException;

    /**
     * Import an entire study from the indicated PACS to this XNAT instance in a single retrieve
     * operation, rather than one operation per series. The series that make up the study can't be
     * filtered, so this is only appropriate when the whole study is wanted.
     *
     * @param pacs  The PACS from which the user wants to import.
     * @param user  The user that launched the request.
     * @param study The study to be imported.
     * @param ae    The AE title the PACS should use when sending the study back to XNAT.
     */
    void importStudy(Pacs pacs, UserI user, Study study, String ae) throws DqrException;

    /**
     * Import the specified series from the indicated PACS to this XNAT instance.
     *
     * @param pacs              The PACS from which the user wants to import.
     * @param studyInstanceUid  The study containing the desired instance.
     * @param seriesInstanceUid The series containing the desired instance.
     * @param sopInstanceUid    The instance to be imported.
     * @param destinationAe     The AE title the PACS should use when sending the instance back to XNAT.
     */
    void importInstance(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, String destinationAe) throws PacsException;

    /**
     * Export the indicated {@link XnatImagescandata series} to the specified PACS.
     *
     * @param pacs   The PACS to which the user wants to export.
     * @param series The series to be exported to the PACS.
     */
    void exportSeries(Pacs pacs, XnatImagescandata series);
}
