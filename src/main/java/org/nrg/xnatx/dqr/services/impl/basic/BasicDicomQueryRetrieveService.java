/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.basic.BasicDicomQueryRetrieveService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.basic;

import lombok.AccessLevel;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import javax.validation.constraints.NotNull;
import org.nrg.config.services.ConfigService;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.dcm.scp.exceptions.UnknownDicomScpInstanceException;
import org.nrg.framework.constants.Scope;
import org.nrg.xapi.exceptions.DataFormatException;
import org.nrg.xapi.exceptions.InitializationException;
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatImagesessiondata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xft.event.EventMetaI;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.entities.ArchiveProcessorInstance;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.processor.services.ArchiveProcessorInstanceService;
import org.nrg.xnatx.dqr.dicom.RetrieveLevel;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreFailureException;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.dto.PacsImportRequest;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.dto.StudyImportInformation;
import org.nrg.xnatx.dqr.exceptions.ArchiveProcessorsNotAvailableException;
import org.nrg.xnatx.dqr.exceptions.CsvPacsRequestTooManyRowsException;
import org.nrg.xnatx.dqr.exceptions.DicomReceiverCustomProcessingDisabledException;
import org.nrg.xnatx.dqr.exceptions.DqrException;
import org.nrg.xnatx.dqr.exceptions.PacsException;
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;
import org.nrg.xnatx.dqr.messaging.PacsSearchRequest;
import org.nrg.xnatx.dqr.messaging.PacsSessionExportRequest;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;
import org.nrg.xnatx.dqr.services.PacsClientRoutingService;
import org.nrg.xnatx.dqr.services.PacsClientService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.nrg.xnatx.dqr.utils.AeTitle;
import org.nrg.xnatx.dqr.utils.CsvRow;
import org.nrg.xnatx.dqr.utils.Dcm4cheUtils;
import org.nrg.xnatx.dqr.utils.DqrDateRange;
import org.nrg.xnatx.dqr.utils.FindRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Consumer;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class BasicDicomQueryRetrieveService implements DicomQueryRetrieveService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public BasicDicomQueryRetrieveService(
            final DicomSCPManager dicomSCPManager,
            final QueuedPacsRequestService queuedPacsRequestService,
            final ConfigService configService,
            final StudyRoutingService studyRoutingService,
            final PacsService pacsService,
            final JmsTemplate jmsTemplate,
            final ArchiveProcessorInstanceService archiveProcessorInstanceService,
            final XnatUserProvider primaryAdminUserProvider,
            final PacsClientRoutingService pacsClientRoutingService
    ) {
        _dicomSCPManager                 = dicomSCPManager;
        _queuedPacsRequestService        = queuedPacsRequestService;
        _configService                   = configService;
        _studyRoutingService             = studyRoutingService;
        _pacsService                     = pacsService;
        _jmsTemplate                     = jmsTemplate;
        _archiveProcessorInstanceService = archiveProcessorInstanceService;
        _xnatUserProvider                = primaryAdminUserProvider;
        _searchCache                     = new HashMap<>();
        _pacsClientRoutingService        = pacsClientRoutingService;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean ping(final UserI user, final Pacs pacs) {
        return _pacsClientRoutingService.getPacsClientService(pacs).ping(pacs);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Patient> getPatientsByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria criteria) throws PacsException {
        return _pacsClientRoutingService.getPacsClientService(pacs).queryPatients(pacs, criteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Study> getStudiesByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria criteria) throws PacsException, DataFormatException {
        return _pacsClientRoutingService.getPacsClientService(pacs).queryStudies(pacs, criteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Study> getStudyById(final UserI user, final Pacs pacs, final String studyInstanceUid) throws PacsException {
        return _pacsClientRoutingService.getPacsClientService(pacs).getStudy(pacs, studyInstanceUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> findStudyInstanceUids(final Pacs pacs, final Map<Integer, String> keys) throws PacsException {
        if (log.isDebugEnabled()) {
            log.debug("Preparing to query PACS {} {} for studies matching keys:\n * {}", pacs.getId(), pacs.getLabel(),
                    keys.entrySet().stream().map(entry -> Dcm4cheUtils.getTagName(entry.getKey()) + "=" + entry.getValue()).collect(Collectors.joining("\n * ")));
        }

        final Map<Integer, String> searchKeys = new HashMap<>(keys);
        searchKeys.putIfAbsent(Tag.StudyInstanceUID, null);

        final List<String> studyInstanceUids = new ArrayList<>();
        _pacsClientRoutingService.getPacsClientService(pacs)
                .queryStudies(pacs, searchKeys, attributes -> studyInstanceUids.add(attributes.getString(Tag.StudyInstanceUID)));

        if (log.isDebugEnabled()) {
            log.debug("Found {} study instance UIDs on PACS {} {}: {}", studyInstanceUids.size(), pacs.getId(), pacs.getLabel(), String.join(", ", studyInstanceUids));
        }
        return studyInstanceUids;
    }


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
    private PacsSearchResults<Series> getSeriesByStudy(final UserI user, final Pacs pacs, final Study study) throws PacsException {
        return study == null ? PacsSearchResults.emptyResults() : getSeriesByStudyUid(user, pacs, study.getStudyInstanceUid());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Series> getSeriesByStudyUid(final UserI user, final Pacs pacs, final String studyUid) throws PacsException {
        return _pacsClientRoutingService.getPacsClientService(pacs).querySeries(pacs, PacsSearchCriteria.builder().studyInstanceUid(studyUid).build());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, PacsSearchResults<Series>> getSeriesByStudyUid(final UserI user, final Pacs pacs, final List<String> studyUids) throws PacsException {
        final PacsClientService pacsClientService = _pacsClientRoutingService.getPacsClientService(pacs);
        final Map<String, PacsSearchResults<Series>> results = new HashMap<>();
        for (final String studyUid : studyUids) {
            results.put(studyUid, pacsClientService.querySeries(pacs, PacsSearchCriteria.builder().studyInstanceUid(studyUid).build()));
        }
        return results;
    }

    @Override
    public Attributes getInstanceMetadata(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, Map<Integer, String> searchKeys) throws PacsException {
        return _pacsClientRoutingService.getPacsClientService(pacs)
                .getInstanceMetadata(pacs, studyInstanceUid, seriesInstanceUid, sopInstanceUid, searchKeys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<String> findSeriesInstanceUids(final Pacs pacs, final String studyInstanceUid) throws PacsException {
        log.debug("Preparing to query PACS {} {} for series instance UIDs matching study instance UID {}",
                pacs.getId(), pacs.getLabel(), studyInstanceUid);

        final List<String> seriesInstanceUids = new ArrayList<>();
        _pacsClientRoutingService.getPacsClientService(pacs)
                .querySeries(pacs, studyInstanceUid, Collections.emptyMap(), attributes -> seriesInstanceUids.add(attributes.getString(Tag.SeriesInstanceUID)));

        if (log.isDebugEnabled()) {
            log.debug("Found {} series instance UIDs from PACS {} {} matching study instance UID {}: {}",
                    seriesInstanceUids.size(), pacs.getId(), pacs.getLabel(), studyInstanceUid,
                    String.join(", ", seriesInstanceUids));
        }
        return seriesInstanceUids;
    }

    @Override
    public void querySeriesInstances(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, Map<Integer, String> searchKeys, Consumer<Attributes> callback) throws PacsException {
        _pacsClientRoutingService.getPacsClientService(pacs).queryInstance(pacs, studyInstanceUid, seriesInstanceUid, searchKeys, callback);
    }

    @Override
    public List<String> findSopInstanceUids(Pacs pacs, String studyInstanceUid, String seriesInstanceUid) throws PacsException {
        final List<String> sopInstanceUids = new ArrayList<>();
        querySeriesInstances(pacs, studyInstanceUid, seriesInstanceUid, Collections.emptyMap(), (attributes) -> sopInstanceUids.add(attributes.getString(Tag.SOPInstanceUID)));
        return sopInstanceUids;
    }

    @Override
    public Attributes getInstanceMetadata(Pacs pacs, String retrieveUrl, Map<Integer, String> searchKeys) throws PacsException {
        return _pacsClientRoutingService.getPacsClientService(pacs).getInstanceMetadata(pacs, retrieveUrl, searchKeys);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getSearchStatus(final UUID requestId) throws NotFoundException {
        if (!_searchCache.containsKey(requestId)) {
            throw new NotFoundException("No search request found for UUID " + requestId);
        }
        final Pair<PacsSearchRequest, Map<String, PacsSearchResults<Series>>> entry = _searchCache.get(requestId);
        return entry.getKey().getStudyInstanceUids().size() > entry.getValue().size();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void updateSearchResults(final UUID requestId, final String studyInstanceUid, final PacsSearchResults<Series> results) throws NotFoundException {
        if (!_searchCache.containsKey(requestId)) {
            throw new NotFoundException("No search request found for UUID " + requestId);
        }
        final Map<String, PacsSearchResults<Series>> aggregate = _searchCache.get(requestId).getValue();
        aggregate.put(studyInstanceUid, results);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, PacsSearchResults<Series>> getSearchResults(final UUID requestId) throws NotFoundException {
        return getSearchStatus(requestId) ? _searchCache.remove(requestId).getValue() : Collections.emptyMap();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importSeries(final UserI user, final Pacs pacs, final Study study, final Series series, final String ae) throws DqrException {
        _pacsClientRoutingService.getPacsClientService(pacs).importSeries(pacs, user, study, series, ae);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importStudy(final UserI user, final Pacs pacs, final Study study, final String ae) throws DqrException {
        _pacsClientRoutingService.getPacsClientService(pacs).importStudy(pacs, user, study, ae);
    }

    @Override
    public void importInstance(Pacs pacs, String studyInstanceUid, String seriesInstanceUid, String sopInstanceUid, String destinationAe) throws PacsException {
        _pacsClientRoutingService.getPacsClientService(pacs).importInstance(pacs, studyInstanceUid, seriesInstanceUid, sopInstanceUid, destinationAe);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importFromPacsRequest(final ExecutedPacsRequest request, UserI user) throws DqrException {
        final Pacs pacs = _pacsService.retrieve(request.getPacsId());
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(request.getPacsId());
        }

        final String aeAndPort = request.getDecodedAeAndPort();
        if (!pacs.isDicomWebEnabled() && !doesScpReceiverExist(aeAndPort)) {
            throw new PacsNotStorableException(new AeTitle(aeAndPort));
        }
        final String aeTitle = StringUtils.substringBefore(aeAndPort, ":");
        final PacsClientService pacsClientService = _pacsClientRoutingService.getPacsClientService(pacs);
        final Study study = assignStudyToProject(request.getXnatProject(), request.getStudyInstanceUid(),
                request.getSubjectLabel(), request.getExperimentLabel(), request.getUsername());

        // The level was resolved when the request was queued. Anything that needs a subset of a
        // study was forced to SERIES then, so a STUDY request here always wants the whole study.
        if (request.getRetrieveLevel() == RetrieveLevel.STUDY) {
            log.debug("Requesting all of study instance UID {} from PACS {} in a single retrieve", request.getStudyInstanceUid(), request.getPacsId());
            pacsClientService.importStudy(pacs, user, study, aeTitle);
            return;
        }

        for (final String seriesId : request.getSeriesIds()) {
            log.debug("Requesting series {} for study instance UID {}", seriesId, request.getStudyInstanceUid());
            pacsClientService.importSeries(pacs, user, study, Series.builder().seriesInstanceUid(seriesId).build(), aeTitle);
        }
    }

    @Override
    public Integer exportSession(final UserI user, final Pacs pacs, final XnatImagesessiondata session, final List<String> scanIds) throws InitializationException {
        final Integer workflowId = createExportWorkflow(user, pacs, session, scanIds);
        _jmsTemplate.convertAndSend("pacsSessionExportRequest", PacsSessionExportRequest.builder().requestingUser(user.getUsername()).pacs(pacs).dateRequested(new Date()).sessionId(session.getId()).scanIds(scanIds).workflowId(workflowId).build());
        return workflowId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean exportSeries(final UserI user, final Pacs pacs, final XnatImagescandata series) {
        try {
            _pacsClientRoutingService.getPacsClientService(pacs).exportSeries(pacs, series);
            return true;
        } catch (CStoreFailureException e) {
            log.error("An error occurred trying to export series {} from image session {} to PACS {}", series.getId(), series.getImageSessionId(), pacs.getId(), e);
            return false;
        }
    }

    /**
     * Indicates whether XNAT has an enabled DICOM SCP receiver with the given AE title
     *
     * @param ae The AE title and optional port of the DICOM receiver to check.
     */
    private boolean doesScpReceiverExist(final String ae) {
        final boolean hasPort = ae.contains(":");
        return _dicomSCPManager.getDicomSCPInstances().values().stream()
                .filter(DicomSCPInstance::isEnabled)
                .map(scp -> hasPort ? scp.getAeTitle() + ":" + scp.getPort() : scp.getAeTitle())
                .anyMatch(scpAe -> StringUtils.equalsIgnoreCase(ae, scpAe));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<FindRow> extractNewImportRequestFromCsv(final UserI requestingUser, final File csv, final long pacsId, final boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        return _extractImportRequestFromCsv(requestingUser, csv, pacsId, allowRowThatGetsAllStudiesOnPacs, true, FIND_ROW_PROCESSOR);
    }

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public List<QueuedPacsRequest> importFromPacs(final UserI user, final PacsImportRequest request) throws PacsNotFoundException, DicomReceiverCustomProcessingDisabledException, UnknownDicomScpInstanceException, NotFoundException, ArchiveProcessorsNotAvailableException, PacsNotQueryableException {
        // Map<String, StudyImportInformation> studiesToImport, String ae, String project, , boolean importEvenIfCustomProcessingIsOff
        final long pacsId = request.getPacsId();
        final Pacs pacs   = _pacsService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(pacsId);
        }

        final List<StudyImportInformation>  studies      = request.getStudies();
        final boolean                       isMultiStudy = studies.size() > 1;
        final Map<String, Optional<String>> anonScripts  = studies.stream().collect(Collectors.toMap(StudyImportInformation::getStudyInstanceUid, BasicDicomQueryRetrieveService::getAnonScript));
        if (!pacs.isDicomWebEnabled() && !request.isForceImport() && anonScripts.values().stream().anyMatch(Optional::isPresent)) {
            validateDicomScpInstance(request.getAeTitle(), request.getPort());
        }
        final RetrieveLevel retrieveLevel = RetrieveLevel.resolve(request.getRetrieveLevel(), pacs.getRetrieveLevel());
        log.debug("Importing {} studies from PACS {} at the {} level", studies.size(), pacsId, retrieveLevel);

        return studies.stream()
                .map(studyInfo -> queueStudyImport(user, pacs, request.getProjectId(), request.getAeTitle(), request.getPort(), isMultiStudy, studyInfo, anonScripts.get(studyInfo.getStudyInstanceUid()), request.getRequestId(), retrieveLevel))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean processSpreadsheetImportFromRows(final UserI user, final List<CsvRow> rows, final String ae, final String project, final long pacsId, final boolean importEvenIfCustomProcessingIsOff) throws Exception {
        final Pacs pacs = _pacsService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }
        final AeTitle            aeTitle       = new AeTitle(ae);
        final AtomicBoolean      valueToReturn = new AtomicBoolean(true);
        final Map<Study, String> studies       = new HashMap<>();
        for (final CsvRow row : rows) {
            if (row != null && row.getStudies() != null) {
                for (final Study study : row.getStudies()) {
                    if (study != null && !studies.containsKey(study)) {
                        final String anonScript = row.getAnonScript();
                        studies.put(study, anonScript);
                        if (StringUtils.isNotBlank(anonScript) && !importEvenIfCustomProcessingIsOff) {
                            validateDicomScpInstance(aeTitle.getAeTitle(), aeTitle.getPort());
                        }
                    }
                }
            }
        }
        final boolean multiStudy = studies.size() > 1;
        for (final Study study : studies.keySet()) {
            final String anonScript = studies.get(study);
            final String login      = _xnatUserProvider.getLogin();
            final String studyId    = study.getStudyInstanceUid();
            final String path       = "/studies/" + studyId;
            log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, studyId);
            if (studyId == null) {
                _configService.replaceConfig(login, "", DicomEdit.ToolName, path, anonScript);
            } else {
                _configService.replaceConfig(login, "", DicomEdit.ToolName, path, anonScript, Scope.Site, studyId);
                _configService.enable(login, "", DicomEdit.ToolName, path, Scope.Site, studyId);
            }

            final List<String> seriesInstanceUids;
            try {
                seriesInstanceUids = getSeriesByStudy(user, pacs, study).getResults().stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList());
            } catch (PacsException e) {
                log.error("Could not query PACS {} for series", pacs.getId(), e);
                continue;
            }
            final QueuedPacsRequest queuedPacsRequest = createQueuedPacsRequest(
                    user,
                    aeTitle.toString(),
                    project,
                    pacsId,
                    multiStudy,
                    studyId,
                    seriesInstanceUids,
                    null,
                    RetrieveLevel.resolve(null, pacs.getRetrieveLevel()));
            _queuedPacsRequestService.create(queuedPacsRequest);
            valueToReturn.set(false);
        }
        return valueToReturn.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processSpreadsheetImport(final UserI user, final File csv, final String ae, final String project, final long pacsId) throws PacsNotFoundException {
        // TODO: The processSpreadsheetImport*() methods need refactoring similar to extract*ImportRequestFromCsv() methods to eliminate duplicate code
        final Pacs pacs = _pacsService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }

        final Map<Study, String> studiesListMappedToAnonScript = new HashMap<>();
        try {
            final RowManager           indexes   = new RowManager(FileUtils.CSVFileToArrayList(csv));
            final Map<Integer, String> columnMap = indexes.getColumnMap();

            indexes.forEach(row -> {
                final PacsSearchCriteria.PacsSearchCriteriaBuilder searchCriteriaBuilder = PacsSearchCriteria.builder();
                if (row.hasStudyInstanceUid()) {
                    searchCriteriaBuilder.studyInstanceUid(row.getStudyInstanceUid());
                }
                if (row.hasAccessionNumber()) {
                    searchCriteriaBuilder.accessionNumber(row.getAccessionNumber());
                }
                if (row.hasFirstName() || row.hasLastName()) {
                    final String lastName  = StringUtils.defaultIfBlank(row.getLastName(), "");
                    final String firstName = StringUtils.defaultIfBlank(row.getFirstName(), "");
                    searchCriteriaBuilder.patientName(StringUtils.isNotBlank(firstName) ? lastName + "," + firstName : lastName);
                }
                if (row.hasPatientId()) {
                    searchCriteriaBuilder.patientName(row.getPatientId());
                }
                if (row.hasStudyDate()) {
                    getDateRange(row.getStudyDate()).ifPresent(searchCriteriaBuilder::studyDateRange);
                }
                if (row.hasDob()) {
                    searchCriteriaBuilder.dob(row.getDob());
                }
                if (row.hasModality()) {
                    searchCriteriaBuilder.modality(row.getModality());
                }

                final Optional<String> script = getAnonScript(columnMap, row);
                try {
                    getStudiesByExample(user, pacs, searchCriteriaBuilder.build()).getResults().stream()
                                                                                  .filter(Objects::nonNull)
                                                                                  .filter(study -> !studiesListMappedToAnonScript.containsKey(study))
                                                                                  .forEach(study -> studiesListMappedToAnonScript.put(study, script.orElse(null)));
                } catch (DataFormatException e) {
                    log.warn("An error occurred trying to query the PACS {}, returning null for search results", pacs.getLabel(), e);
                } catch (PacsException e) {
                    log.warn("The PACS {} is not queryable, returning null for search results", pacs.getLabel(), e);
                }
            });
        } catch (final Throwable e) {
            log.error("Failed to get studies list from spreadsheet.", e);
        }

        final Set<Map.Entry<Study, String>> studiesSet = studiesListMappedToAnonScript.entrySet();
        final boolean                       multiStudy = studiesSet.size() > 1;
        for (final Map.Entry<Study, String> entry : studiesSet) {
            final Study  study      = entry.getKey();
            final String anonScript = entry.getValue();

            final String studyInstanceUid = study.getStudyInstanceUid();
            final List<String> seriesInstanceUids;
            try {
                seriesInstanceUids = getSeriesByStudy(user, pacs, study).getResults().stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList());
            } catch (PacsException e) {
                log.error("Could not query PACS {} for series", pacs.getId(), e);
                continue;
            }
            final QueuedPacsRequest queuedPacsRequest = createQueuedPacsRequest(
                    user,
                    ae,
                    project,
                    pacsId,
                    multiStudy,
                    studyInstanceUid,
                    seriesInstanceUids,
                    null,
                    RetrieveLevel.resolve(null, pacs.getRetrieveLevel()));
            queuedPacsRequest.setRemappingScript(anonScript);
            _queuedPacsRequestService.create(queuedPacsRequest);
        }
    }

    protected Study assignStudyToProject(final String projectId, final String studyInstanceUid, final String username) {
        return assignStudyToProject(projectId, studyInstanceUid, null, null, username);
    }

    protected Study assignStudyToProject(final String projectId, final String studyInstanceUid,
                                          final String subjectLabel, final String experimentLabel,
                                          final String username) {
        if (!StringUtils.isBlank(projectId)) {
            log.debug("Assigning study instance UID {} to project {} (subject={}, experiment={})",
                    studyInstanceUid, projectId, subjectLabel, experimentLabel);
            _studyRoutingService.assign(studyInstanceUid, projectId, subjectLabel, experimentLabel, username);
            return new Study(projectId, studyInstanceUid);
        } else {
            log.debug("No project assignment specified for study instance UID {}, may be registered as Unassigned", studyInstanceUid);
            return new Study(studyInstanceUid);
        }
    }

    private interface RowProcessor<R> {
        R process(final Map<Integer, String> columnMap, final List<String> row, final PacsSearchCriteria criteria, final Collection<Study> results);
    }

    private Optional<QueuedPacsRequest> queueStudyImport(final UserI user, final Pacs pacs, final String projectId, final String aeTitle, final int port, final boolean isMultiStudy, final StudyImportInformation studyInfo, final Optional<String> anonScript, final String requestId, final RetrieveLevel retrieveLevel) {
        final String            studyInstanceUid   = studyInfo.getStudyInstanceUid();
        final List<String>      seriesDescriptions = studyInfo.getSeriesDescriptions();
        final List<String>      seriesInstanceUids = studyInfo.getSeriesInstanceUids();
        final Predicate<Series> includeSeries      = new IncludeSeries(seriesInstanceUids, seriesDescriptions);

        //TODO: We should just be able to uncomment the setStudyScript call and remove the 11 lines below it, but I'm having a build issue with the updated XNAT code not being picked up. This should be changed as soon as those issues are resolved.
        final String login = _xnatUserProvider.getLogin();
        log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, studyInstanceUid);

        final List<Series> results;
        try {
            results = getSeriesByStudyUid(user, pacs, studyInstanceUid).getResults().stream().filter(includeSeries).collect(Collectors.toList());
        } catch (PacsException e) {
            log.warn("The PACS " + pacs.getId() + " is not currently queryable");
            return Optional.empty();
        }

        if (!results.isEmpty()) {
            final String ae = port == 0 ? aeTitle : aeTitle + ":" + port;
            final Series            first       = results.get(0);
            final QueuedPacsRequest pacsRequest = createQueuedPacsRequest(user, ae, projectId, pacs.getId(), isMultiStudy, studyInstanceUid, results.stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList()), requestId, effectiveRetrieveLevel(retrieveLevel, studyInfo));
            pacsRequest.setStudyDate(first.getStudyDate());
            pacsRequest.setStudyId(first.getStudyId());
            pacsRequest.setAccessionNumber(first.getAccessionNumber());
            pacsRequest.setPatientId(first.getPatientId());
            pacsRequest.setPatientName(first.getPatientName());

            anonScript.ifPresent(pacsRequest::setRemappingScript);

            return Optional.of(_queuedPacsRequestService.create(pacsRequest));
        }
        return Optional.empty();
    }

    @NotNull
    private QueuedPacsRequest createQueuedPacsRequest(final UserI user, final String ae, final String project, final long pacsId, final boolean multiStudy, final String studyId, final List<String> seriesIds, final String requestId, final RetrieveLevel retrieveLevel) {
        final QueuedPacsRequest request = QueuedPacsRequest.builder()
                                .pacsId(pacsId)
                                .username(user.getUsername())
                                .xnatProject(project)
                                .studyInstanceUid(studyId)
                                .seriesIds(seriesIds)
                                .destinationAeTitle(ae)
                                .priority(multiStudy ? PacsRequest.STANDARD_PRIORITY : PacsRequest.HIGH_PRIORITY)
                                .status(PacsRequest.QUEUED_STATUS_TEXT)
                                .requestId(requestId)
                                .queuedTime(new Date()).build();
        request.setRetrieveLevel(retrieveLevel);
        return request;
    }

    /**
     * Determines the level a single study is retrieved at. A study that names the series to import
     * has to be retrieved series by series whatever the configured level says, because a study-level
     * retrieve pulls the whole study and can't filter it.
     *
     * @param requested The level resolved for the request as a whole.
     * @param studyInfo The study being queued.
     *
     * @return The level to use for this study.
     */
    private static RetrieveLevel effectiveRetrieveLevel(final RetrieveLevel requested, final StudyImportInformation studyInfo) {
        if (requested != RetrieveLevel.STUDY) {
            return requested;
        }
        if (CollectionUtils.isEmpty(studyInfo.getSeriesInstanceUids()) && CollectionUtils.isEmpty(studyInfo.getSeriesDescriptions())) {
            return RetrieveLevel.STUDY;
        }
        log.info("Study {} was requested at the STUDY level but names the series to import, so it will be retrieved at the SERIES level instead", studyInfo.getStudyInstanceUid());
        return RetrieveLevel.SERIES;
    }

    private Integer createExportWorkflow(final UserI user, final Pacs pacs, final XnatImagesessiondata session, final List<String> scanIds) throws InitializationException {
        try {
            final PersistentWorkflowI workflow = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, session.getId(), session.getProject(), EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "EXPORT_TO_PACS_REQUEST", null, "Pacs: " + pacs.getId() + ", session: " + session.getId() + ", scans: " + String.join(", ", scanIds)));
            assert workflow != null;
            workflow.setStatus("Queued");
            final EventMetaI event = workflow.buildEvent();
            PersistentWorkflowUtils.save(workflow, event);
            return workflow.getWorkflowId();
        } catch (Exception e) {
            throw new InitializationException("An error occurred trying to create a workflow for request by user " + user.getUsername() + " to export session " + session.getId() + " to PACS " + pacs.getId() + ": " + String.join(", ", scanIds));
        }
    }

    private <R> List<R> _extractImportRequestFromCsv(final UserI user, final File csv, final long pacsId, final boolean allowRowThatGetsAllStudiesOnPacs, final boolean isNewRequest, final RowProcessor<R> processor) throws Exception {
        final Pacs pacs = _pacsService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }

        final RowManager           indexes   = new RowManager(FileUtils.CSVFileToArrayList(csv).stream().filter(list -> !list.isEmpty()).collect(Collectors.toList()));
        final Map<Integer, String> columnMap = indexes.getColumnMap(isNewRequest);

        if (indexes.size() > 1000) {
            throw new CsvPacsRequestTooManyRowsException("For performance reasons, please limit the number of rows in your uploaded CSV to 1000.");
        }

        return indexes.stream().map(row -> {
            final AtomicBoolean                                areThereSearchCriteriaForThisRow = new AtomicBoolean();
            final PacsSearchCriteria.PacsSearchCriteriaBuilder searchCriteriaBuilder            = PacsSearchCriteria.builder();
            if (row.hasStudyInstanceUid()) {
                searchCriteriaBuilder.studyInstanceUid(isNewRequest ? removeExtraQuotes(row.getStudyInstanceUid()) : row.getStudyInstanceUid());
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (row.hasAccessionNumber()) {
                searchCriteriaBuilder.accessionNumber(isNewRequest ? removeExtraQuotes(row.getAccessionNumber()) : row.getAccessionNumber());
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (row.hasFirstName() || row.hasLastName()) {
                final String firstName = row.getFirstName();
                final String lastName  = row.getLastName();
                final String fullName  = StringUtils.isNotBlank(firstName) ? lastName + "," + firstName : lastName;
                searchCriteriaBuilder.patientName(isNewRequest ? removeExtraQuotes(fullName) : fullName);
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (row.hasPatientId()) {
                searchCriteriaBuilder.patientId(isNewRequest ? removeExtraQuotes(row.getPatientId()) : row.getPatientId());
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (row.hasStudyDate()) {
                final Optional<DqrDateRange> dateRange = getDateRange(row.getStudyDate());
                if (dateRange.isPresent()) {
                    searchCriteriaBuilder.studyDateRange(dateRange.get());
                    areThereSearchCriteriaForThisRow.set(true);
                }
            }
            if (row.hasDob()) {
                searchCriteriaBuilder.dob(isNewRequest ? removeExtraQuotes(row.getDob()) : row.getDob());
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (row.hasModality()) {
                searchCriteriaBuilder.modality(isNewRequest ? removeExtraQuotes(row.getModality()) : row.getModality());
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (!areThereSearchCriteriaForThisRow.get() && !allowRowThatGetsAllStudiesOnPacs) {
                log.error("No search criteria found for row. Users must specify at least one valid search criteria.");
                return null;
            }

            final PacsSearchCriteria criteria = searchCriteriaBuilder.build();

            log.debug("User {} is querying PACS {}: {}", user.getUsername(), pacs.getId(), criteria);
            final PacsSearchResults<Study> studies;
            try {
                studies = getStudiesByExample(user, pacs, criteria);
            } catch (PacsNotQueryableException e) {
                // This should never happen: we should have already accounted for this situation.
                log.error("PACS {} is not queryable, no results will be returned.", e.getPacsId(), e);
                return null;
            } catch (PacsException e) {
                log.error("PACS {} error", e.getPacsId(), e);
                return null;
            } catch (DataFormatException e) {
                log.error("The submitted PACS query criteria were invalid", e);
                return null;
            }

            final Collection<Study> results = studies.getResults();
            log.debug("User {} queried PACS {} with {} results", user.getUsername(), pacs.getId(), results.size());

            return processor.process(columnMap, row, criteria, results);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private void validateDicomScpInstance(final String aeTitle, final int port) throws NotFoundException, UnknownDicomScpInstanceException, DicomReceiverCustomProcessingDisabledException, ArchiveProcessorsNotAvailableException {
        final DicomSCPInstance scpInstance = _dicomSCPManager.getDicomSCPInstance(aeTitle, port);
        if (!scpInstance.isEnabled()) {
            throw new UnknownDicomScpInstanceException(aeTitle + ":" + port);
        }
        if (!scpInstance.isCustomProcessing()) {
            throw new DicomReceiverCustomProcessingDisabledException(scpInstance);
        }
        final List<ArchiveProcessorInstance> processorInstances = _archiveProcessorInstanceService.getAllEnabledSiteProcessorsForAe(aeTitle + ":" + port);
        if (processorInstances.isEmpty() || processorInstances.stream().allMatch(instance -> StringUtils.equals(instance.getProcessorClass(), "org.nrg.xnat.processors.MizerArchiveProcessor"))) {
            throw new ArchiveProcessorsNotAvailableException(scpInstance);
        }
    }

    private static class IncludeSeries implements Predicate<Series> {
        private final List<String> _seriesInstanceUids;
        private final List<String> _seriesDescriptions;
        private final boolean      _isFilteredBySeriesInstanceUids;
        private final boolean      _isFilteredBySeriesDescriptions;

        IncludeSeries(final List<String> seriesInstanceUids, final List<String> seriesDescriptions) {
            _seriesInstanceUids             = seriesInstanceUids;
            _seriesDescriptions             = seriesDescriptions;
            _isFilteredBySeriesInstanceUids = !CollectionUtils.isEmpty(seriesInstanceUids);
            _isFilteredBySeriesDescriptions = !CollectionUtils.isEmpty(seriesDescriptions);
        }

        @Override
        public boolean test(final Series series) {
            return (!_isFilteredBySeriesInstanceUids || _seriesInstanceUids.contains(StringUtils.defaultIfBlank(series.getSeriesInstanceUid(), ""))) &&
                   (!_isFilteredBySeriesDescriptions || _seriesDescriptions.contains(StringUtils.defaultIfBlank(series.getSeriesDescription(), "")));
        }
    }

    private static String removeExtraQuotes(String inputString) {
        return StringUtils.equals(inputString, CLEAR_SIGNIFIER_3X) ? CLEAR_SIGNIFIER : inputString;
    }

    private static Optional<DqrDateRange> getDateRange(final String studyDate) {
        if (!studyDate.contains("-")) {
            return Optional.of(new DqrDateRange(studyDate));
        }

        final String startDate = StringUtils.substringBefore(studyDate, "-");
        final String endDate   = StringUtils.substringAfter(studyDate, "-");

        // If all blank, range is open on both ends so no search criteria should be added.
        return StringUtils.isAllBlank(startDate, endDate) ? Optional.empty() : Optional.of(new DqrDateRange(startDate, endDate));
    }

    private static Optional<String> getAnonScript(final Map<Integer, String> columns, final List<String> row) {
        final String script = columns.entrySet().stream().filter(entry -> StringUtils.isNotBlank(entry.getValue())).map(entry -> {
            final String mapped = row.get(entry.getKey());
            return StringUtils.equalsAny(mapped, CLEAR_SIGNIFIER, CLEAR_SIGNIFIER_3X) ? entry.getValue() + " := \"\"" : entry.getValue() + " := \"" + mapped + "\"";
        }).collect(Collectors.joining(System.lineSeparator()));
        return StringUtils.isBlank(script) ? Optional.empty() : Optional.of("version \"6.1\"" + System.lineSeparator() + script);
    }

    private static Optional<String> getAnonScript(final StudyImportInformation info) {
        final String script = info.getAnonScript();
        if (StringUtils.isNotBlank(script)) {
            return Optional.of(script);
        }
        final Map<String, String> map = info.getRelabelMap();
        if (map.isEmpty()) {
            return Optional.empty();
        }
        return Optional.of("version \"6.1\"" + System.lineSeparator() +
                           map.entrySet()
                              .stream()
                              .filter(entry -> StringUtils.isNotBlank(entry.getValue()))
                              .map(entry -> {
                                  final String[] tags   = StringUtils.split(HEADER_TO_TAG_MAP.get(entry.getKey()), ":");
                                  final String   value  = entry.getValue();
                                  final String   assign = StringUtils.equalsAny(value, CLEAR_SIGNIFIER, CLEAR_SIGNIFIER_3X) ? " := \"\"" : " := \"" + value + "\"";
                                  return Arrays.stream(tags).map(tag -> tag + assign).collect(Collectors.toList());
                              }).flatMap(Collection::stream).collect(Collectors.joining(System.lineSeparator())));
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class Row extends ArrayList<String> {
        private static final long serialVersionUID = 6044778440958597758L;

        private final int studyInstanceUidIndex;
        private final int accessionNumberIndex;
        private final int studyDateIndex;
        private final int patientIdIndex;
        private final int lastNameIndex;
        private final int firstNameIndex;
        private final int dobIndex;
        private final int modalityIndex;

        Row(final List<String> values, final List<String> columnHeaders) {
            super(values);
            studyInstanceUidIndex = columnHeaders.indexOf("Study Instance UID");
            accessionNumberIndex  = columnHeaders.indexOf("Accession Number");
            studyDateIndex        = columnHeaders.indexOf("Study Date");
            patientIdIndex        = columnHeaders.indexOf("Patient ID");
            lastNameIndex         = columnHeaders.indexOf("Last Name");
            firstNameIndex        = columnHeaders.indexOf("First Name");
            dobIndex              = columnHeaders.indexOf("DOB");
            modalityIndex         = columnHeaders.indexOf("Modality");
        }

        public boolean hasStudyInstanceUid() {
            return studyInstanceUidIndex >= 0 && StringUtils.isNotBlank(getStudyInstanceUid());
        }

        String getStudyInstanceUid() {
            return get(studyInstanceUidIndex);
        }

        public boolean hasAccessionNumber() {
            return accessionNumberIndex >= 0 && StringUtils.isNotBlank(getAccessionNumber());
        }

        String getAccessionNumber() {
            return get(accessionNumberIndex);
        }

        boolean hasStudyDate() {
            return studyDateIndex >= 0 && StringUtils.isNotBlank(getStudyDate());
        }

        String getStudyDate() {
            return get(studyDateIndex);
        }

        boolean hasPatientId() {
            return patientIdIndex >= 0 && StringUtils.isNotBlank(getPatientId());
        }

        String getPatientId() {
            return get(patientIdIndex);
        }

        boolean hasLastName() {
            return lastNameIndex >= 0 && StringUtils.isNotBlank(getLastName());
        }

        String getLastName() {
            return get(lastNameIndex);
        }

        boolean hasFirstName() {
            return firstNameIndex >= 0 && StringUtils.isNotBlank(getFirstName());
        }

        String getFirstName() {
            return get(firstNameIndex);
        }

        boolean hasDob() {
            return dobIndex >= 0 && StringUtils.isNotBlank(getDob());
        }

        String getDob() {
            return get(dobIndex);
        }

        boolean hasModality() {
            return modalityIndex >= 0 && StringUtils.isNotBlank(getModality());
        }

        String getModality() {
            return get(modalityIndex);
        }
    }

    @Data
    @EqualsAndHashCode(callSuper = true)
    private static class RowManager extends ArrayList<Row> {
        private static final long serialVersionUID = 5979173538127275825L;

        RowManager(final List<List<String>> rows) {
            columnHeaders = rows.get(0);
            addAll(rows.subList(1, rows.size()).stream().map(row -> new Row(row, columnHeaders)).collect(Collectors.toList()));
        }

        public Map<Integer, String> getColumnMap() {
            return getColumnMap(false);
        }

        public Map<Integer, String> getColumnMap(final boolean isNewRequest) {
            return HEADER_TO_TAG_MAP.entrySet().stream().filter(entry -> columnHeaders.contains(entry.getKey())).collect(Collectors.toMap(entry -> columnHeaders.indexOf(entry.getKey()), entry -> isNewRequest ? entry.getKey() : entry.getValue()));
        }

        @Getter(AccessLevel.NONE)
        private List<String> columnHeaders;
        @Getter(AccessLevel.NONE)
        private List<String> row;
    }

    private static final String                CLEAR_SIGNIFIER    = "\"\"";
    private static final String                CLEAR_SIGNIFIER_3X = CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER;
    private static final Map<String, String>   HEADER_TO_TAG_MAP  = Stream.of(new String[][]{{"Relabel Accession Number", "(0008,0050)"},
                                                                                             {"Relabel Study Date", "(0008,0020)"},
                                                                                             {"Relabel Study ID", "(0020,0010)"},
                                                                                             {"Relabel Patient ID", "(0010,0020)"},
                                                                                             {"Relabel Patient Name", "(0010,0010)"},
                                                                                             {"Relabel Patient Birth Date", "(0010,0030)"},
                                                                                             {"Subject", "(0010,0010):(0010,0020)"},
                                                                                             {"Session", "(0020,0010):(0008,0050)"}}).collect(Collectors.toMap(entry -> entry[0], entry -> entry[1]));
    private static final RowProcessor<FindRow> FIND_ROW_PROCESSOR = (columnMap, row, criteria, results) -> FindRow.builder().criteria(criteria).relabelMap(columnMap.entrySet().stream()
                                                                                                                                                                    .filter(entry -> StringUtils.isNotBlank(row.get(entry.getKey())))
                                                                                                                                                                    .collect(Collectors.toMap(Map.Entry::getValue, entry -> {
                                                                                                                                                                        final String value = row.get(entry.getKey());
                                                                                                                                                                        return StringUtils.equalsAny(value, CLEAR_SIGNIFIER, CLEAR_SIGNIFIER_3X) ? "\"\"" : value;
                                                                                                                                                                    }))).studies(results).build();

    private final DicomSCPManager                                                            _dicomSCPManager;
    private final QueuedPacsRequestService                                                   _queuedPacsRequestService;
    private final ConfigService                                                              _configService;
    private final StudyRoutingService                                                        _studyRoutingService;
    private final PacsService                                                                _pacsService;
    private final JmsTemplate                                                                _jmsTemplate;
    private final ArchiveProcessorInstanceService                                            _archiveProcessorInstanceService;
    private final XnatUserProvider                                                           _xnatUserProvider;
    private final Map<UUID, Pair<PacsSearchRequest, Map<String, PacsSearchResults<Series>>>> _searchCache;
    private final PacsClientRoutingService                                                   _pacsClientRoutingService;
}
