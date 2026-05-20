package org.nrg.xnatx.dqr.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.http.client.utils.URIBuilder;
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
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyDateRangeLimitResults;
import org.nrg.xnatx.dqr.exceptions.DqrException;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.exceptions.PacsDataNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.DicomWebCredentialService;
import org.nrg.xnatx.dqr.services.PacsClientService;
import org.nrg.xnatx.dqr.utils.DqrConstants;
import org.nrg.xnatx.dqr.utils.DqrDateRange;
import org.springframework.stereotype.Service;

import java.net.URI;
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
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Slf4j
@Service
public class DicomWebPacsClientService implements PacsClientService {
    private static final String     STUDIES_ENDPOINT       = "studies";
    private static final String     SERIES_ENDPOINT        = "series";
    private static final String     INSTANCES_ENDPOINT     = "instances";
    private static final String     META_DATA_ENDPOINT     = "metadata";

    private static final Integer[] PATIENT_QUERY_TAGS = new Integer[] {
            Tag.PatientID, Tag.PatientName, Tag.PatientBirthDate, Tag.PatientSex
    };
    private static final Integer[] STUDY_QUERY_TAGS = new Integer[] {
            Tag.StudyID, Tag.StudyDescription, Tag.AccessionNumber, Tag.StudyDate,
            Tag.ReferringPhysicianName, Tag.ModalitiesInStudy,

    };
    private static final Integer[] SERIES_QUERY_TAGS = new Integer[] {
            Tag.SeriesInstanceUID, Tag.StudyInstanceUID, Tag.Modality, Tag.SeriesDescription, Tag.SeriesNumber,
            Tag.StudyDate, Tag.StudyID, Tag.AccessionNumber, Tag.PatientID, Tag.PatientName,
    };

    private final DicomWebCredentialService dicomWebCredentialService;
    private final OrmStrategy ormStrategy;
    private final ConcurrentMap<Pacs, DicomWebHttpClient> dicomWebHttpClients;
    private final DicomFileNamer defaultFileName;
    private final Map<String, DicomObjectIdentifier<XnatProjectdata>> dicomObjectIdentifiers;
    private final DqrPreferences dqrPreferences;

    public DicomWebPacsClientService(final DicomWebCredentialService dicomWebCredentialService,
                                     final DicomFileNamer fileNamer,
                                     final Map<String, DicomObjectIdentifier<XnatProjectdata>> dicomObjectIdentifiers,
                                     final OrmStrategy ormStrategy,
                                     final DqrPreferences dqrPreferences) {
        this.dicomWebCredentialService = dicomWebCredentialService;
        this.defaultFileName = fileNamer;
        this.dicomObjectIdentifiers = dicomObjectIdentifiers;
        this.ormStrategy = ormStrategy;
        this.dqrPreferences = dqrPreferences;
        dicomWebHttpClients = new ConcurrentHashMap<>();

    }

    @Override
    public boolean ping(Pacs pacs) {
        return getDicomWebHttpClient(pacs).ping().isSuccessful();
    }

    @Override
    public Optional<Study> getStudy(Pacs pacs, String studyId) throws PacsException {
        if (StringUtils.isBlank(studyId)) {
            return Optional.empty();
        }

        final Map<Integer, String> searchKeys = Stream.concat(Arrays.stream(STUDY_QUERY_TAGS), Arrays.stream(PATIENT_QUERY_TAGS))
                .map(tag -> new AbstractMap.SimpleEntry<>(tag, (String) null))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
        searchKeys.put(Tag.StudyInstanceUID, studyId);

        final List<Study> studies = queryStudies(pacs, searchKeys);
        return studies.isEmpty() ? Optional.empty() : Optional.of(studies.get(0));
    }

    @Override
    public PacsSearchResults<Patient> queryPatients(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        // There is no such thing as a patient-level query in DICOMweb,
        // so we'll just query for studies and then extract the patients from them
        final PacsSearchResults<Study> studyResults = queryStudies(pacs, searchCriteria);
        final List<Patient> patients = studyResults.getResults().stream()
                .map(Study::getPatient)
                .distinct()
                .collect(Collectors.toList());
        return PacsSearchResults.<Patient>builder()
                .results(patients)
                .hasLimitedResultSetSize(false)
                .studyDateRangeLimitResults(studyResults.getStudyDateRangeLimitResults())
                .build();
    }

    @Override
    public PacsSearchResults<Study> queryStudies(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        // Build initial map of search keys from criteria
        // NOTE: These are dcm4che2 tags, not dcm4che3 tags
        final Map<Integer, String> searchKeys = searchCriteria.getDicomKeysMap();

        // Ensure we have all the other tags we need to build a Study
        Stream.concat(Arrays.stream(STUDY_QUERY_TAGS), Arrays.stream(PATIENT_QUERY_TAGS))
                .map(tag -> new AbstractMap.SimpleEntry<>(tag, (String) null))
                .forEach(entry -> searchKeys.putIfAbsent(entry.getKey(), entry.getValue()));

        // Get the date range limit
        final StudyDateRangeLimitResults studyDateRangeLimitResults = ormStrategy.getResultSetLimitStrategy().limitStudyDateRange(searchCriteria);
        final DqrDateRange studyDateRange = studyDateRangeLimitResults.getDateRange();
        if (studyDateRange != null && studyDateRange.isBounded()) {
            log.debug("Limiting study date range to {}", studyDateRange.getStudyDateCriterion());
            searchKeys.put(Tag.StudyDate, studyDateRange.getStudyDateCriterion());
        }

        // Get patient name search criteria
        final List<String> patientNameSearchCriteria = ormStrategy.getPatientNameStrategy()
                .dqrSearchCriteriaToDicomSearchCriteria(searchCriteria)
                .getCriteriaInOrderOfPreference().stream()
                .filter(StringUtils::isNotBlank)
                .collect(Collectors.toList());

        List<Study> studies = Collections.emptyList();
        if (patientNameSearchCriteria.isEmpty()) {
            // No patient name criteria, so just make the query
            studies = queryStudies(pacs, searchKeys);
        } else {
            // Make potentially multiple queries, one for each patient name criterion, until we find studies
            for (final String patientNameCriterion : patientNameSearchCriteria) {
                log.debug("Querying for studies matching patient name {}", patientNameCriterion);
                searchKeys.put(Tag.PatientName, patientNameCriterion);
                studies = queryStudies(pacs, searchKeys);
                if (!studies.isEmpty()) {
                    break;
                }
            }
        }
        return PacsSearchResults.<Study>builder()
                .results(studies)
                .hasLimitedResultSetSize(false)
                .studyDateRangeLimitResults(studyDateRangeLimitResults)
                .build();
    }

    @Override
    public PacsSearchResults<Series> querySeries(Pacs pacs, PacsSearchCriteria searchCriteria) throws PacsException {
        // Build initial map of search keys from criteria
        // NOTE: These are dcm4che2 tags, not dcm4che3 tags
        final Map<Integer, String> searchKeys = searchCriteria.getDicomKeysMap();

        // Ensure we have all the tags we need to build a Series
        Arrays.stream(SERIES_QUERY_TAGS)
                .map(tag -> new AbstractMap.SimpleEntry<>(tag, (String) null))
                .forEach(entry -> searchKeys.putIfAbsent(entry.getKey(), entry.getValue()));

        final List<Series> series = new ArrayList<>();
        querySeries(pacs, searchKeys, attributes -> series.add(Series.from(attributes)));
        return PacsSearchResults.<Series>builder()
                .results(series)
                .hasLimitedResultSetSize(false)
                .studyDateRangeLimitResults(null)
                .build();
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

    private List<Study> queryStudies(Pacs pacs, Map<Integer, String> searchKeys) throws PacsException {
        final List<Study> studies = new ArrayList<>();
        queryStudies(pacs, searchKeys, attributes -> studies.add(Study.from(attributes, ormStrategy.getPatientNameStrategy()::dicomPatientNameToDqrPatientName)));
        return studies;
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

    /**
     * {@inheritDoc}
     */
    @Override
    public Attributes getInstanceMetadata(Pacs pacs, String retrieveUrl, Map<Integer, String> searchKeys) throws PacsException {
        URIBuilder retrieveUriBuilder = new URIBuilder(URI.create(retrieveUrl));
        final List<String> pathSegments = retrieveUriBuilder.getPathSegments();
        pathSegments.add(META_DATA_ENDPOINT);
        final URIBuilder metadataUriBuilder = retrieveUriBuilder.setPathSegments(pathSegments);
        return getDicomWebHttpClient(pacs).getAttributes(metadataUriBuilder, searchKeys);
    }

    @Override
    public void importSeries(final Pacs pacs, final UserI user, final Study study, final Series series, final String ae) throws DqrException {
        log.info("Importing study {} series {}", study.getStudyInstanceUid(), series.getSeriesInstanceUid());

        log.debug("Querying for RetrieveURL for study {} series {}.",
                study.getStudyInstanceUid(), series.getSeriesInstanceUid());
        final String[] retrieveUrlHolder = new String[1];
        final Map<Integer, String> searchKeys = new HashMap<>();
        searchKeys.put(Tag.SeriesInstanceUID, series.getSeriesInstanceUid());
        searchKeys.put(Tag.RetrieveURL, null);
        querySeries(pacs,
                study.getStudyInstanceUid(),
                searchKeys,
                attributes -> retrieveUrlHolder[0] = attributes.getString(Tag.RetrieveURL));
        final String retrieveUrl = retrieveUrlHolder[0];
        if (StringUtils.isBlank(retrieveUrl)) {
            throw new PacsDataNotFoundException("Could not find series");
        }
        // Retrieve
        getDicomWebHttpClient(pacs).getItem(retrieveUrl, Collections.emptyMap(),
                (partNumber, multipartInputStream) ->
                        importSeriesFromMultipartDicom(pacs, user, study.getProjectId(), ae, partNumber, multipartInputStream)
        );
        log.info("Study {} series {} imported", study.getStudyInstanceUid(), series.getSeriesInstanceUid());
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
        return dicomWebHttpClients.computeIfAbsent(pacs, p -> new DicomWebHttpClient(pacs.getDicomWebRootUrl(), dicomWebCredentialService.getCredential(pacs.getAeTitle()).orElse(null), dqrPreferences));
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
            parameters.put(DqrConstants.DICOM_IMPORT_PARAM_DQR_DICOM_WEB, true);
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
