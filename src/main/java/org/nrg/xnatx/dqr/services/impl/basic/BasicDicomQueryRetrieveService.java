/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.basic.BasicDicomQueryRetrieveService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.basic;

import lombok.*;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.tuple.Pair;
import org.jetbrains.annotations.NotNull;
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
import org.nrg.xnatx.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.CFindSCU;
import org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.Dcm4cheToolCFindSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveSCU;
import org.nrg.xnatx.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.xnatx.dqr.dicom.command.cmove.dcm4che.tool.Dcm4cheToolCMoveSCU;
import org.nrg.xnatx.dqr.dicom.command.cstore.BasicCStoreSCU;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreFailureException;
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.CMoveRequestContext;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.DimseRequestContext;
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
import org.nrg.xnatx.dqr.exceptions.*;
import org.nrg.xnatx.dqr.messaging.PacsSearchRequest;
import org.nrg.xnatx.dqr.messaging.PacsSessionExportRequest;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.nrg.xnatx.dqr.services.SeriesRetrievalRequestService;
import org.nrg.xnatx.dqr.utils.AeTitle;
import org.nrg.xnatx.dqr.utils.CsvRow;
import org.nrg.xnatx.dqr.utils.DqrDateRange;
import org.nrg.xnatx.dqr.utils.FindRow;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import javax.annotation.Nullable;
import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Slf4j
public class BasicDicomQueryRetrieveService implements DicomQueryRetrieveService {
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    @Autowired
    public BasicDicomQueryRetrieveService(final DqrPreferences preferences,
                                          final DicomSCPManager dicomSCPManager,
                                          final QueuedPacsRequestService queuedPacsRequestService,
                                          final ConfigService configService,
                                          final StudyRoutingService studyRoutingService,
                                          final PacsService pacsService,
                                          final JmsTemplate jmsTemplate,
                                          final ArchiveProcessorInstanceService archiveProcessorInstanceService,
                                          final XnatUserProvider primaryAdminUserProvider,
                                          final SeriesRetrievalRequestService seriesRetrievalRequestService,
                                          final Map<String, OrmStrategy> ormStrategies) {
        _preferences                     = preferences;
        _dicomSCPManager                 = dicomSCPManager;
        _queuedPacsRequestService        = queuedPacsRequestService;
        _configService                   = configService;
        _studyRoutingService             = studyRoutingService;
        _pacsService                     = pacsService;
        _jmsTemplate                     = jmsTemplate;
        _archiveProcessorInstanceService = archiveProcessorInstanceService;
        _xnatUserProvider                = primaryAdminUserProvider;
        _seriesRetrievalRequestService   = seriesRetrievalRequestService;
        _ormStrategies                   = ormStrategies;
        _searchCache                     = new HashMap<>();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean canConnect(final UserI user, final Pacs pacs) {
        return buildCEchoSCU(pacs).canConnect();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Patient> getPatientsByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria criteria) throws PacsNotQueryableException {
        final DimseRequestContext context = new CMoveRequestContext(_seriesRetrievalRequestService, user, null);
        return buildCFindSCU(context, pacs).cfindPatientsByExample(criteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Patient> getPatientById(final UserI user, final Pacs pacs, final String patientId) throws PacsNotQueryableException {
        final DimseRequestContext context = new CMoveRequestContext(_seriesRetrievalRequestService, user, null);
        return buildCFindSCU(context, pacs).cfindPatientById(patientId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Study> getStudiesByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria criteria) throws PacsNotQueryableException, DataFormatException {
        final DimseRequestContext context = new CMoveRequestContext(_seriesRetrievalRequestService, user, null);
        try {
            return buildCFindSCU(context, pacs).cfindStudiesByExample(criteria);
        } catch (DqrRuntimeException e) {
            throw new DataFormatException("A DQR run-time exception occurred, which usually indicates a problem performing a query to the PACS: " + Optional.ofNullable(e.getCause()).orElse(e).getMessage() + "\n\nThe search criteria for this query is: " + criteria, e);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Study> getStudyById(final UserI user, final Pacs pacs, final String studyInstanceUid) throws PacsNotQueryableException {
        final DimseRequestContext context = new CMoveRequestContext(_seriesRetrievalRequestService, user, null);
        return buildCFindSCU(context, pacs).cfindStudyById(studyInstanceUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Series> getSeriesByStudy(final UserI user, final Pacs pacs, final Study study) throws PacsNotQueryableException {
        final DimseRequestContext context = new CMoveRequestContext(_seriesRetrievalRequestService, user, null);
        return buildCFindSCU(context, pacs).cfindSeriesByStudy(study);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Series> getSeriesByStudyUid(final UserI user, final Pacs pacs, final String studyUid) throws PacsNotQueryableException {
        final DimseRequestContext context = new CMoveRequestContext(_seriesRetrievalRequestService, user, null);
        return buildCFindSCU(context, pacs).cfindSeriesByStudyUid(studyUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, PacsSearchResults<Series>> getSeriesByStudyUid(final UserI user, final Pacs pacs, final List<String> studyUids) throws PacsNotQueryableException {
        final DimseRequestContext context = new CMoveRequestContext(_seriesRetrievalRequestService, user, null);
        final CFindSCU findSCU = buildCFindSCU(context, pacs);
        return studyUids.stream().collect(Collectors.toMap(Function.identity(), findSCU::cfindSeriesByStudyUid));
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
    public PacsSearchRequest getSearchRequest(final UUID requestId) throws NotFoundException {
        if (!_searchCache.containsKey(requestId)) {
            throw new NotFoundException("No search request found for UUID " + requestId);
        }

        return _searchCache.get(requestId).getKey();
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
    public UUID getSeriesByStudyUidAsync(final UserI user, final Pacs pacs, final List<String> studyUids) throws PacsNotQueryableException {
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(pacs.getId());
        }

        final PacsSearchRequest request  = PacsSearchRequest.builder().username(user.getUsername()).pacsId(pacs.getId()).studyInstanceUids(studyUids).build();
        final UUID              searchId = request.getSearchId();
        _searchCache.put(searchId, Pair.of(request, new HashMap<>()));
        sendPacsSearchRequest(request);
        return searchId;
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
    public Optional<Series> getSeriesById(final UserI user, final Pacs pacs, final String seriesInstanceUid) throws PacsNotQueryableException {
        final DimseRequestContext context = new CMoveRequestContext(_seriesRetrievalRequestService, user, null);
        return buildCFindSCU(context, pacs).cfindSeriesById(seriesInstanceUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importSeries(final DimseRequestContext context, final Pacs pacs, final Study study, final Series series, final String ae) {
        buildCMoveSCU(pacs, ae).cmoveSeries(context, study, series);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importFromPacsRequest(final UserI user, final ExecutedPacsRequest request) throws PacsNotQueryableException, PacsNotStorableException {
        final Pacs pacs = _pacsService.retrieve(request.getPacsId());
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(request.getPacsId());
        }
        final String aeAndPort = request.getDecodedAeAndPort();
        if (!isAeStorable(aeAndPort)) {
            throw new PacsNotStorableException(new AeTitle(aeAndPort));
        }
        final String aeTitle = StringUtils.substringBefore(aeAndPort, ":");
        final DimseRequestContext context = new CMoveRequestContext(_seriesRetrievalRequestService, user, request.getUserDefinedId());
        try {
            final Study study = assignStudyToProject(request.getXnatProject(), request.getStudyInstanceUid(), request.getUsername());
            for (final String seriesId : request.getSeriesIds()) {
                log.debug("Requesting series {} for study instance UID {}", seriesId, request.getStudyInstanceUid());
                buildCMoveSCU(pacs, aeTitle).cmoveSeries(context, study, Series.builder().seriesInstanceUid(seriesId).build());
            }
        } catch (final CMoveTargetNotFoundException exception) {
            log.warn("C-MOVE target not found somehow: PACS {}", pacs, exception);
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
            buildCStoreSCU(pacs).cstoreSeries(series);
            return true;
        } catch (CStoreFailureException e) {
            log.error("An error occurred trying to export series {} from image session {} to PACS {}", series.getId(), series.getImageSessionId(), pacs.getId(), e);
            return false;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isAeStorable(final String ae) {
        //The user is able to store to an AE if there is either an XNAT SCP receiver with that AE or there is an enabled PACS with that AE for which storable=true
        final boolean hasPort = ae.contains(":");
        return _dicomSCPManager.getDicomSCPInstances().values().stream().anyMatch(scp -> scp.isEnabled() && StringUtils.equalsIgnoreCase(ae, hasPort ? scp.getAeTitle() + ":" + scp.getPort() : scp.getAeTitle()));
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
     */
    @Override
    public List<CsvRow> extractImportRequestFromCsv(final UserI requestingUser, final File csv, final long pacsId, final boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        return _extractImportRequestFromCsv(requestingUser, csv, pacsId, allowRowThatGetsAllStudiesOnPacs, false, CSV_ROW_PROCESSOR);
    }

    /*
     * @param studiesToImport                   The studies to import.
     * @param ae                                The AE receiving the data to be imported.
     * @param project                           The project to which the data should be imported.
     * @param pacsId                            The PACS from which the data should be imported.
     * @param importEvenIfCustomProcessingIsOff Indicates that the data should be imported even if the AE doesn't currently support custom processing.
     */

    /**
     * {@inheritDoc}
     *
     * @return
     */
    @Override
    public List<QueuedPacsRequest> importFromPacs(final UserI user, final @Nullable String userDefinedId, final PacsImportRequest request) throws PacsNotFoundException, DicomReceiverCustomProcessingDisabledException, UnknownDicomScpInstanceException, NotFoundException, ArchiveProcessorsNotAvailableException, PacsNotQueryableException {
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
        if (!request.isForceImport() && anonScripts.values().stream().anyMatch(Optional::isPresent)) {
            validateDicomScpInstance(request.getAeTitle(), request.getPort());
        }
        return request.getStudies().stream().map(studyInfo -> queueStudyImport(user, userDefinedId, pacs, request.getProjectId(), request.getAeTitle(), request.getPort(), isMultiStudy, studyInfo, anonScripts.get(studyInfo.getStudyInstanceUid()))).filter(Optional::isPresent).map(Optional::get).collect(Collectors.toList());
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean processSpreadsheetImportFromRows(final UserI user, final String userDefinedId, final List<CsvRow> rows, final String ae, final String project, final long pacsId, final boolean importEvenIfCustomProcessingIsOff) throws Exception {
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

            try {
                _queuedPacsRequestService.create(createQueuedPacsRequest(user, userDefinedId, aeTitle.toString(), project, pacsId, multiStudy, studyId, getSeriesByStudy(user, pacs, study).getResults().stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList())));
                valueToReturn.set(false);
            } catch (Exception e) {
                if (e instanceof CMoveFailureException) {
                    log.error("C-MOVE operation failed: {}", e.getMessage(), e);
                } else if (e.getCause() instanceof CMoveFailureException) {
                    log.error("C-MOVE operation failed: {}", e.getCause().getMessage(), e.getCause());
                } else {
                    log.error("An unexpected error occurred", e);
                }
            }
        }
        return valueToReturn.get();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void processSpreadsheetImport(final UserI user, final String userDefinedId, final File csv, final String ae, final String project, final long pacsId) throws PacsNotFoundException {
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
                } catch (PacsNotQueryableException e) {
                    log.warn("The PACS {} is not queryable, returning null for search results", pacs.getLabel(), e);
                } catch (DataFormatException e) {
                    log.warn("An error occurred trying to query the PACS {}, returning null for search results", pacs.getLabel(), e);
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
            try {
                final QueuedPacsRequest request = createQueuedPacsRequest(user, userDefinedId, ae, project, pacsId, multiStudy, studyInstanceUid, getSeriesByStudy(user, pacs, study).getResults().stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList()));
                request.setRemappingScript(anonScript);
                _queuedPacsRequestService.create(request);
            } catch (PacsNotQueryableException e) {
                log.warn("The PACS {} is not queryable, request not created for the study {}", pacs.getLabel(), studyInstanceUid, e);
            } catch (CMoveFailureException e) {
                log.error("C-MOVE operation failed: {}", e.getMessage(), e);
            } catch (Exception e) {
                if (e.getCause() instanceof CMoveFailureException) {
                    log.error("C-MOVE operation failed: {}", e.getCause().getMessage(), e.getCause());
                } else {
                    log.error("An unexpected error occurred", e);
                }
            }
        }
    }

    protected Study assignStudyToProject(final String projectId, final String studyInstanceUid, final String username) {
        if (!StringUtils.isBlank(projectId)) {
            log.debug("Assigning study instance UID {} to project {}", studyInstanceUid, projectId);
            _studyRoutingService.assign(studyInstanceUid, projectId, username);
            return new Study(projectId, studyInstanceUid);
        } else {
            log.debug("No project assignment specified for study instance UID {}, may be registered as Unassigned", studyInstanceUid);
            return new Study(studyInstanceUid);
        }
    }

    private interface RowProcessor<R> {
        R process(final Map<Integer, String> columnMap, final List<String> row, final PacsSearchCriteria criteria, final Collection<Study> results);
    }

    private Optional<QueuedPacsRequest> queueStudyImport(final UserI user, final @Nullable String userDefinedId, final Pacs pacs, final String projectId, final String aeTitle, final int port, final boolean isMultiStudy, final StudyImportInformation studyInfo, final Optional<String> anonScript) {
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
        } catch (PacsNotQueryableException e) {
            log.warn("The PACS " + pacs.getId() + " is not currently queryable");
            return Optional.empty();
        }

        if (!results.isEmpty()) {
            try {
                final Series            first       = results.get(0);
                final QueuedPacsRequest pacsRequest = createQueuedPacsRequest(user, userDefinedId, aeTitle + ":" + port, projectId, pacs.getId(), isMultiStudy, studyInstanceUid, results.stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList()));
                pacsRequest.setStudyDate(first.getStudyDate());
                pacsRequest.setStudyId(first.getStudyId());
                pacsRequest.setAccessionNumber(first.getAccessionNumber());
                pacsRequest.setPatientId(first.getPatientId());
                pacsRequest.setPatientName(first.getPatientName());

                anonScript.ifPresent(pacsRequest::setRemappingScript);

                return Optional.of(_queuedPacsRequestService.create(pacsRequest));
            } catch (Exception e) {
                if (e instanceof CMoveFailureException) {
                    log.error("C-MOVE operation failed: {}", e.getMessage(), e);
                } else if (e.getCause() instanceof CMoveFailureException) {
                    log.error("C-MOVE operation failed: {}", e.getCause().getMessage(), e.getCause());
                } else {
                    log.error("An unexpected error occurred", e);
                }
            }
        }
        return Optional.empty();
    }

    @NotNull
    private QueuedPacsRequest createQueuedPacsRequest(final UserI user, final @Nullable String userDefinedId, final String ae, final String project, final long pacsId, final boolean multiStudy, final String studyId, final List<String> seriesIds) {
        return QueuedPacsRequest.builder()
                                .pacsId(pacsId)
                                .username(user.getUsername())
                                .userDefinedId(userDefinedId)
                                .xnatProject(project)
                                .studyInstanceUid(studyId)
                                .seriesIds(seriesIds)
                                .destinationAeTitle(ae)
                                .priority(multiStudy ? PacsRequest.STANDARD_PRIORITY : PacsRequest.HIGH_PRIORITY)
                                .status(PacsRequest.QUEUED_STATUS_TEXT)
                                .queuedTime(new Date()).build();
    }

    @SuppressWarnings("unused")
    private void getCurrentSeries(final StudyInfo.StudyInfoBuilder builder, final AtomicBoolean extraStudyInfoSet, final List<String> seriesInstanceUIDs, final List<String> seriesToImport, final Series currSeries, final String result) {
        if (seriesInstanceUIDs.contains(result)) {
            seriesToImport.add(result);
            if (!extraStudyInfoSet.get()) {
                builder.studyDate(currSeries.getStudyDate())
                       .studyId(currSeries.getStudyId())
                       .accessionNumber(currSeries.getAccessionNumber())
                       .patientId(currSeries.getPatientId())
                       .patientName(currSeries.getPatientName());
                extraStudyInfoSet.set(true);
            }
        }
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
            } catch (DataFormatException e) {
                log.error("The submitted PACS query criteria were invalid", e);
                return null;
            }

            final Collection<Study> results = studies.getResults();
            log.debug("User {} queried PACS {} with {} results", user.getUsername(), pacs.getId(), results.size());

            return processor.process(columnMap, row, criteria, results);
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private CEchoSCU buildCEchoSCU(final Pacs pacs) {
        return new Dcm4cheToolCEchoSCU(_preferences, buildDicomConnectionProperties(pacs));
    }

    private CFindSCU buildCFindSCU(final DimseRequestContext context, final Pacs pacs) throws PacsNotQueryableException {
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(pacs.getId());
        }
        return new Dcm4cheToolCFindSCU(_preferences, context, buildDicomConnectionProperties(pacs), getOrmStrategy(pacs));
    }

    private CStoreSCU buildCStoreSCU(final Pacs pacs) {
        return new BasicCStoreSCU(_preferences, buildDicomConnectionProperties(pacs));
    }

    private CMoveSCU buildCMoveSCU(final Pacs pacs, final String receiverAETitle) {
        return new Dcm4cheToolCMoveSCU(_preferences, buildDicomConnectionProperties(pacs, receiverAETitle), getOrmStrategy(pacs));
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

    private DicomConnectionProperties buildDicomConnectionProperties(final Pacs pacs) {
        // At some point in the future caller will probably specify AE as well
        // For now, this is an ugly hack that just uses the first defined AE in the XNAT webapp
        DicomSCPInstance firstXnatScp = _dicomSCPManager.getDicomSCPInstances()
                                                        .values().iterator().next();
        final String localAETitle = firstXnatScp.getAeTitle();
        return new DicomConnectionProperties(localAETitle, pacs);
    }

    private DicomConnectionProperties buildDicomConnectionProperties(final Pacs pacs, final String receiverAETitle) {
        if (!StringUtils.isBlank(receiverAETitle)) {
            return new DicomConnectionProperties(receiverAETitle, pacs);
        } else {
            return buildDicomConnectionProperties(pacs);
        }
    }

    private void sendPacsSearchRequest(final PacsSearchRequest pacsSearchRequest) {
        _jmsTemplate.convertAndSend("pacsSearchRequest", pacsSearchRequest);
    }

    private OrmStrategy getOrmStrategy(final Pacs pacs) {
        final String beanId = pacs.getOrmStrategySpringBeanId();
        if (!_ormStrategies.containsKey(beanId)) {
            throw new DqrRuntimeException(String.format("Failed to load the ORM strategy defined by bean '%s'", pacs.getOrmStrategySpringBeanId()));
        }
        return _ormStrategies.get(beanId);
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

    @Value
    @Builder
    private static class StudyInfo {
        @Builder.Default
        String studyDate       = null;
        @Builder.Default
        String studyId         = null;
        @Builder.Default
        String accessionNumber = null;
        @Builder.Default
        String patientId       = null;
        @Builder.Default
        String patientName     = null;
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
    private static final RowProcessor<CsvRow>  CSV_ROW_PROCESSOR  = (columnMap, row, criteria, results) -> CsvRow.builder().criteria(criteria).anonScript(getAnonScript(columnMap, row).orElse(null)).studies(results).build();

    private final DqrPreferences                                                             _preferences;
    private final DicomSCPManager                                                            _dicomSCPManager;
    private final QueuedPacsRequestService                                                   _queuedPacsRequestService;
    private final ConfigService                                                              _configService;
    private final StudyRoutingService                                                        _studyRoutingService;
    private final PacsService                                                                _pacsService;
    private final JmsTemplate                                                                _jmsTemplate;
    private final ArchiveProcessorInstanceService                                            _archiveProcessorInstanceService;
    private final XnatUserProvider                                                           _xnatUserProvider;
    private final SeriesRetrievalRequestService _seriesRetrievalRequestService;
    private final Map<String, OrmStrategy>                                                   _ormStrategies;
    private final Map<UUID, Pair<PacsSearchRequest, Map<String, PacsSearchResults<Series>>>> _searchCache;
}
