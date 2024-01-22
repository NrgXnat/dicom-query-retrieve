package org.nrg.xnatx.dqr.services;

import org.dcm4che3.data.Attributes;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xdat.model.XnatImagescandataI;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;

import java.util.Collection;
import java.util.Optional;

public interface PacsClientService {
    /**
     * Indicates whether XNAT can connect to the indicated PACS system.
     *
     * @param pacs The PACS to which the user wants to connect.
     *
     * @return Returns <b>true</b> if XNAT can connect to the specified PACS, <b>false</b> otherwise.
     */
    boolean canConnect(Pacs pacs);

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

    // New versions using dcm4che >2
    Collection<Patient> queryPatients(Pacs pacs, Attributes searchCriteria) throws PacsException;
    Collection<Study> queryStudies(Pacs pacs, Attributes searchCriteria) throws PacsException;
    Collection<Series> querySeries(Pacs pacs, Attributes searchCriteria) throws PacsException;

    /**
     * Import the specified series from the indicated PACS to this XNAT instance.
     *
     * @param pacs   The PACS from which the user wants to import.
     * @param study  The study containing the desired series.
     * @param series The series to be imported.
     * @param ae     The AE title the PACS should use when sending the series back to XNAT.
     */
    void importSeries(Pacs pacs, Study study, Series series, String ae);

    /**
     * Export the indicated {@link XnatImagescandata series} to the specified PACS.
     *
     * @param pacs   The PACS to which the user wants to export.
     * @param series The series to be exported to the PACS.
     */
    void exportSeries(Pacs pacs, XnatImagescandata series);
}
