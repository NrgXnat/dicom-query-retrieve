package org.nrg.xnatx.dqr.services.impl;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.io.IOUtils;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.dcm4che3.mime.MultipartInputStream;
import org.nrg.xdat.om.XnatImagescandata;
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

import java.nio.charset.StandardCharsets;
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

    public DicomWebPacsClientService(final DicomWebCredentialService dicomWebCredentialService) {
        this.dicomWebCredentialService = dicomWebCredentialService;
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
        getDicomWebHttpClient(pacs).getAttributes(STUDIES_ENDPOINT, searchKeys, callback);
    }

    @Override
    public void querySeries(Pacs pacs, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        querySeries(pacs, searchKeys.get(Tag.StudyInstanceUID), searchKeys, callback);
    }

    @Override
    public void querySeries(Pacs pacs, String studyInstanceUid, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        log.debug("Querying for series matching studyInstanceUid {} {}", studyInstanceUid, searchKeys);
        Objects.requireNonNull(studyInstanceUid, "studyInstanceUid is required");
        final String path = StringUtils.joinWith("/", STUDIES_ENDPOINT, studyInstanceUid, SERIES_ENDPOINT);
        searchKeys = new HashMap<>(searchKeys);
        searchKeys.remove(Tag.StudyInstanceUID);
        getDicomWebHttpClient(pacs).getAttributes(path, searchKeys, callback);
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
        final String path = StringUtils.joinWith("/", STUDIES_ENDPOINT, studyInstanceUid, SERIES_ENDPOINT, seriesInstanceUid, INSTANCES_ENDPOINT);
        searchKeys = new HashMap<>(searchKeys);
        searchKeys.remove(Tag.StudyInstanceUID);
        searchKeys.remove(Tag.SeriesInstanceUID);
        getDicomWebHttpClient(pacs).getAttributes(path, searchKeys, callback);
    }

    @Override
    public void importSeries(Pacs pacs, Study study, Series series, String ae) throws DqrException {
        log.debug("Importing study {} series {}", study.getStudyInstanceUid(), series.getSeriesInstanceUid());
        final String path = StringUtils.joinWith("/",
                STUDIES_ENDPOINT, study.getStudyInstanceUid(), SERIES_ENDPOINT, series.getSeriesInstanceUid()
        );
        getDicomWebHttpClient(pacs).getItem(path, Collections.emptyMap(), this::importSeriesFromMultipartDicom);
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

    private void importSeriesFromMultipartDicom(final int partNumber, final MultipartInputStream multipartInputStream) throws DqrRuntimeException {
        try {
            final Map<String, List<String>> headerParams = multipartInputStream.readHeaderParams();
            final int byteLen = headerParams.get("content-length").stream().mapToInt(Integer::parseInt).sum();
            final int substringLen = Math.min(byteLen, 250);  // I know this is arbitrary, but it's just for debugging
            final String dicomString = IOUtils.toString(multipartInputStream, StandardCharsets.UTF_8);
            log.debug("Part {} headers {} dicom output: {}{}",
                    partNumber, headerParams, dicomString.substring(0, substringLen), substringLen < byteLen ? "..." : ""
            );
        } catch (Exception e) {
            log.error("Error reading DICOMweb response", e);
            // Note: this is a workaround for the callback not being able to throw a checked exception
            throw new DqrRuntimeException(e);
        }
    }
}
