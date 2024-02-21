package org.nrg.xnatx.dqr.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.mime.MultipartInputStream;
import org.nrg.dcm.DicomFileNamer;
import org.nrg.dcm.utils.StreamWrapper;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatProjectdata;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.DicomObjectIdentifier;
import org.nrg.xnat.archive.GradualDicomImporter;
import org.nrg.xnat.helpers.uri.URIManager;
import org.nrg.xnat.restlet.util.FileWriterWrapperI;
import org.nrg.xnatx.dqr.dicom.http.DicomWebHttpClient;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.DqrException;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.services.DicomWebCredentialService;
import org.nrg.xnatx.dqr.services.PacsClientService;
import org.springframework.stereotype.Service;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

@Slf4j
@Service
public class DicomWebPacsClientService implements PacsClientService {
    private static final String     STUDIES_ENDPOINT       = "studies";
    private static final String     SERIES_ENDPOINT        = "series";
    private static final String     INSTANCES_ENDPOINT     = "instances";
    private static final String     META_DATA_ENDPOINT     = "metadata";
    private final DicomWebCredentialService dicomWebCredentialService;
    private final ConcurrentMap<Pacs, DicomWebHttpClient> dicomWebHttpClients;
    private final DicomFileNamer defaultFileName;
    private final Map<String, DicomObjectIdentifier<XnatProjectdata>> dicomObjectIdentifiers;

    public DicomWebPacsClientService(final DicomWebCredentialService dicomWebCredentialService, final DicomFileNamer fileNamer,
                                     final Map<String, DicomObjectIdentifier<XnatProjectdata>> dicomObjectIdentifiers) {
        this.dicomWebCredentialService = dicomWebCredentialService;
        this.defaultFileName = fileNamer;
        this.dicomObjectIdentifiers = dicomObjectIdentifiers;
        dicomWebHttpClients = new ConcurrentHashMap<>();

    }

    @Override
    public boolean canConnect(Pacs pacs) {
        // TODO is there a lightweight way to check that we can connect?
        return true;
    }

    @Override
    public Optional<Study> getStudy(Pacs pacs, String studyId) throws PacsException {
        return Optional.empty();
    }

    @Override
    public PacsSearchResults<Patient> queryPatients(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        return PacsSearchResults.emptyResults();
    }

    @Override
    public PacsSearchResults<Study> queryStudies(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        return PacsSearchResults.emptyResults();
    }

    @Override
    public PacsSearchResults<Series> querySeries(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        return PacsSearchResults.emptyResults();
    }

    @Override
    public void queryPatients(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {

    }

    @Override
    public void queryStudies(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        log.debug("Querying for studies matching {}", searchKeys);
        searchKeys = new HashMap<>(searchKeys);
        getDicomWebHttpClient(pacs).getAttributes(Collections.singletonList(STUDIES_ENDPOINT), searchKeys, callback);
    }

    @Override
    public void querySeries(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        querySeries(pacs, searchKeys.get(Tag.StudyInstanceUID), searchKeys, callback);
    }

    @Override
    public void querySeries(Pacs pacs, String studyInstanceUid, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        log.debug("Querying for series matching studyInstanceUid {} {}", studyInstanceUid, searchKeys);
        Objects.requireNonNull(studyInstanceUid, "studyInstanceUid is required");
        final List<String> pathSegments = Arrays.asList(STUDIES_ENDPOINT, studyInstanceUid, SERIES_ENDPOINT);
        searchKeys = new HashMap<>(searchKeys);
        searchKeys.remove(Tag.StudyInstanceUID);
        getDicomWebHttpClient(pacs).getAttributes(pathSegments, searchKeys, callback);
    }

    @Override
    public void queryInstance(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        queryInstance(pacs, searchKeys.get(Tag.StudyInstanceUID), searchKeys.get(Tag.SeriesInstanceUID), searchKeys, callback);
    }

    @Override
    public void queryInstance(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        log.debug("Querying for instances matching studyInstanceUid {} seriesInstanceUid {} {}", studyInstanceUid, seriesInstanceUid, searchKeys);
        Objects.requireNonNull(studyInstanceUid, "studyInstanceUid is required");
        Objects.requireNonNull(seriesInstanceUid, "seriesInstanceUid is required");
        final List<String> pathSegments = Arrays.asList(
                STUDIES_ENDPOINT, studyInstanceUid, SERIES_ENDPOINT, seriesInstanceUid, INSTANCES_ENDPOINT
        );
        searchKeys = new HashMap<>(searchKeys);
        searchKeys.remove(Tag.StudyInstanceUID);
        searchKeys.remove(Tag.SeriesInstanceUID);
        getDicomWebHttpClient(pacs).getAttributes(pathSegments, searchKeys, callback);
    }

    @Override
    public Attributes getInstanceMetadata(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, Map<Integer, String> searchKeys) throws PacsException {
        log.debug("Getting instance metadata for studyInstanceUid {} seriesInstanceUid {} sopInstanceUid {} search keys {}", studyInstanceUid, seriesInstanceUid, sopInstanceUid, searchKeys);
        Objects.requireNonNull(studyInstanceUid, "studyInstanceUid is required");
        Objects.requireNonNull(seriesInstanceUid, "seriesInstanceUid is required");
        Objects.requireNonNull(sopInstanceUid, "sopInstanceUid is required");
        final List<String> pathSegments = Arrays.asList(
                STUDIES_ENDPOINT, studyInstanceUid, SERIES_ENDPOINT, seriesInstanceUid, INSTANCES_ENDPOINT, sopInstanceUid, META_DATA_ENDPOINT
        );
        searchKeys = new HashMap<>(searchKeys);
        searchKeys.remove(Tag.StudyInstanceUID);
        searchKeys.remove(Tag.SeriesInstanceUID);
        searchKeys.remove(Tag.SOPInstanceUID);
        return getDicomWebHttpClient(pacs).getAttributes(pathSegments, searchKeys);
    }

    @Override
    public void importSeries(final Pacs pacs, final UserI user, final Study study, final Series series, final String ae) throws DqrException {
        log.debug("Importing study {} series {}", study.getStudyInstanceUid(), series.getSeriesInstanceUid());
        final List<String> pathSegments = Arrays.asList(
                STUDIES_ENDPOINT, study.getStudyInstanceUid(), SERIES_ENDPOINT, series.getSeriesInstanceUid()
        );

        getDicomWebHttpClient(pacs).getItem(pathSegments, Collections.emptyMap(), (partNumber, multipartInputStream) -> importSeriesFromMultipartDicom(pacs, user, study.getProjectId(), ae, partNumber, multipartInputStream));
        log.info("Series {} imported", series.getSeriesInstanceUid());
    }


    @Override
    public void importInstance(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, String destinationAe) {
        // This isn't a priority to implement because it's only called by the DIMSE indexing service
    }

    @Override
    public void exportSeries(Pacs pacs, XnatImagescandata series) {
        throw new RuntimeException("Export over DICOMweb not implemented");
    }

    private DicomWebHttpClient getDicomWebHttpClient(final Pacs pacs) {
        return dicomWebHttpClients.computeIfAbsent(pacs, p -> new DicomWebHttpClient(pacs.getDicomWebRootUrl(), dicomWebCredentialService.getCredential(pacs.getAeTitle()).orElse(null)));
    }


    private DicomObjectIdentifier<XnatProjectdata> getDicomObjectIdentifier(final String identifier) {
        return  dicomObjectIdentifiers.get(identifier); //TODO see Line 428 of DicomSCPManager
    }

    /**
    * Modelled on web/org.nrg.dcm.scp.CStoreService doCStore method
     */

    private void importSeriesFromMultipartDicom(final Pacs pacs, final UserI user, final String projectId, final String receiverAeTitle, final int partNumber, final MultipartInputStream multipartInputStream) throws DqrRuntimeException {
        try {
            DicomObjectIdentifier<XnatProjectdata> doi = getDicomObjectIdentifier(pacs.getDicomObjectIdentifier());
            final Map<String, List<String>> headers = multipartInputStream.readHeaderParams();
            log.debug("Importing part {} of multipart DICOM. Headers: {}", partNumber, headers);
            final Map<String, Object> parameters = new HashMap<>();
            parameters.put(GradualDicomImporter.SENDER_ID_PARAM, pacs.getLabel());
            parameters.put(GradualDicomImporter.SENDER_AE_TITLE_PARAM, pacs.getAeTitle());
            parameters.put(GradualDicomImporter.RECEIVER_AE_TITLE_PARAM, receiverAeTitle);
            parameters.put(GradualDicomImporter.CUSTOM_PROC_PARAM, true);
            parameters.put(GradualDicomImporter.DIRECT_ARCHIVE_PARAM, false); //TODO: for now this will be false
            parameters.put(URIManager.PREVENT_ANON, Boolean.toString(!pacs.isAnonymizationEnabled()));
            parameters.put(URIManager.PROJECT_ID, projectId);
            final FileWriterWrapperI fw = new StreamWrapper(multipartInputStream);
            final GradualDicomImporter importer = new GradualDicomImporter(this, user, fw, parameters);
            importer.setIdentifier(doi);
            importer.setNamer(defaultFileName);
            importer.call();
        } catch (Exception e) {
            log.error("Error reading DICOMweb response for {} from {}", partNumber, pacs.getLabel(), e);
            // Note: this is a workaround for the callback not being able to throw a checked exception
            throw new DqrRuntimeException(e);
        }
    }
}
