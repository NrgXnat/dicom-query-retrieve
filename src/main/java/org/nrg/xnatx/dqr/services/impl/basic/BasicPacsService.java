/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.basic.BasicPacsService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.basic;

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
import org.nrg.xapi.exceptions.NotFoundException;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
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
import org.nrg.xnatx.dqr.dicom.command.cstore.CStoreSCU;
import org.nrg.xnatx.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.domain.Patient;
import org.nrg.xnatx.dqr.domain.Series;
import org.nrg.xnatx.dqr.domain.Study;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.dto.PacsSearchCriteria;
import org.nrg.xnatx.dqr.dto.PacsSearchResults;
import org.nrg.xnatx.dqr.exceptions.*;
import org.nrg.xnatx.dqr.messaging.PacsSearchRequest;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.PacsEntityService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.nrg.xnatx.dqr.utils.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class BasicPacsService implements PacsService {
    @Autowired
    @SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection")
    public BasicPacsService(final DqrPreferences preferences, final DicomSCPManager dicomSCPManager, final QueuedPacsRequestService queuedPacsRequestService, final ConfigService configService, final StudyRoutingService studyRoutingService, final PacsEntityService pacsEntityService, final JmsTemplate jmsTemplate, final ArchiveProcessorInstanceService archiveProcessorInstanceService, final XnatUserProvider primaryAdminUserProvider, final Map<String, OrmStrategy> ormStrategies) {
        _preferences = preferences;
        _dicomSCPManager = dicomSCPManager;
        _queuedPacsRequestService = queuedPacsRequestService;
        _configService = configService;
        _studyRoutingService = studyRoutingService;
        _pacsEntityService = pacsEntityService;
        _jmsTemplate = jmsTemplate;
        _archiveProcessorInstanceService = archiveProcessorInstanceService;
        _xnatUserProvider = primaryAdminUserProvider;
        _ormStrategies = ormStrategies;
        _searchCache = new HashMap<>();
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
        return buildCFindSCU(pacs).cfindPatientsByExample(criteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Patient> getPatientById(final UserI user, final Pacs pacs, final String patientId) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindPatientById(patientId);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Study> getStudiesByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria criteria) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindStudiesByExample(criteria);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Study> getStudyById(final UserI user, final Pacs pacs, final String studyInstanceUid) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindStudyById(studyInstanceUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Series> getSeriesByStudy(final UserI user, final Pacs pacs, final Study study) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindSeriesByStudy(study);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public PacsSearchResults<Series> getSeriesByStudyUid(final UserI user, final Pacs pacs, final String studyUid) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindSeriesByStudyUid(studyUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Map<String, PacsSearchResults<Series>> getSeriesByStudyUid(final UserI user, final Pacs pacs, final List<String> studyUids) throws PacsNotQueryableException {
        final CFindSCU findSCU = buildCFindSCU(pacs);
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
        if (!getSearchStatus(requestId)) {
            return null;
        }
        return _searchCache.remove(requestId).getValue();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public Optional<Series> getSeriesById(final UserI user, final Pacs pacs, final String seriesInstanceUid) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindSeriesById(seriesInstanceUid);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importSeries(final UserI user, final Pacs pacs, final Study study, final Series series, final String ae) {
        buildCMoveSCU(pacs, ae).cmoveSeries(study, series);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void importFromPacsRequest(final ExecutedPacsRequest request) throws PacsNotQueryableException, PacsNotStorableException {
        final Pacs pacs = _pacsEntityService.retrieve(request.getPacsId());
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(request.getPacsId());
        }
        final String aeAndPort = request.getDecodedAeAndPort();
        if (!isAeStorable(aeAndPort)) {
            throw new PacsNotStorableException(aeAndPort);
        }
        final String aeTitle = StringUtils.substringBefore(aeAndPort, ":");
        try {
            final Study study = assignStudyToProject(request.getXnatProject(), request.getStudyInstanceUid(), request.getUsername());
            for (final String seriesId : request.getSeriesIds()) {
                log.debug("Requesting series {} for study instance UID {}", seriesId, request.getStudyInstanceUid());
                buildCMoveSCU(pacs, aeTitle).cmoveSeries(study, Series.builder().seriesInstanceUid(seriesId).build());
            }
        } catch (final CMoveTargetNotFoundException exception) {
            log.warn("C-MOVE target not found somehow: PACS {}", pacs, exception);
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public void exportSeries(final UserI user, final Pacs pacs, final XnatImagescandata series) {
        buildCStoreSCU(pacs).cstoreSeries(series);
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
    public List<FindRow> extractNewImportRequestFromCsv(final UserI user, final File csv, final long pacsId, final boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        return _extractImportRequestFromCsv(user, csv, pacsId, allowRowThatGetsAllStudiesOnPacs, true, (user1, pacs, columnMap, row, criteria) -> {
            final Map<String, String> anonMapForThisRow = new HashMap<>();
            for (final Map.Entry<Integer, String> entry : columnMap.entrySet()) {
                final String stringToRemapTo = row.get(entry.getKey());
                if (StringUtils.isNotBlank(stringToRemapTo)) {
                    if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo) || StringUtils.equals(CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER, stringToRemapTo)) {
                        anonMapForThisRow.put(entry.getValue(), "\"\"");
                    } else {
                        anonMapForThisRow.put(entry.getValue(), stringToRemapTo);
                    }
                }
            }

            try {
                return FindRow.builder().criteria(criteria).relabelMap(anonMapForThisRow).studies(getStudiesByExample(user1, pacs, criteria).getResults()).build();
            } catch (PacsNotQueryableException e) {
                log.warn("The PACS {} is not queryable, returning null for search results", pacs.getLabel(), e);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public List<CsvRow> extractImportRequestFromCsv(final UserI user, final File csv, final long pacsId, final boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        return _extractImportRequestFromCsv(user, csv, pacsId, allowRowThatGetsAllStudiesOnPacs, false, (user1, pacs, columnMap, row, criteria) -> {
            final Optional<String> script = getAnonScript(columnMap, row);
            try {
                final PacsSearchResults<Study> results = getStudiesByExample(user1, pacs, criteria);
                return CsvRow.builder().criteria(criteria).anonScript(script.orElse(null)).studies(results.getResults()).build();
            } catch (PacsNotQueryableException e) {
                log.warn("The PACS {} is not queryable, returning null for search results", pacs.getLabel(), e);
                return null;
            }
        });
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean processSpreadsheetImport(Map<String, StudyImportInformation> studiesToImport, UserI user, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws PacsNotFoundException, DicomReceiverCustomProcessingDisabledException, UnknownDicomScpInstanceException, NotFoundException, ArchiveProcessorsNotAvailableException, PacsNotQueryableException {
        final Pacs pacs = _pacsEntityService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }
        final String aeTitle;
        final String port;
        if (StringUtils.contains(ae, ":")) {
            final String[] parts = ae.split(":");
            aeTitle = parts[0];
            port = parts[1];
        } else {
            aeTitle = ae;
            port = "";
        }

        boolean                                        valueToReturn = true;
        Set<Map.Entry<String, StudyImportInformation>> studiesSet    = studiesToImport.entrySet();
        boolean                                        multiStudy    = studiesSet.size() > 1;
        for (Map.Entry<String, StudyImportInformation> studyEntry : studiesSet) {
            String                 currStudy = studyEntry.getKey();
            StudyImportInformation studyInfo = studyEntry.getValue();
            if (currStudy != null) {
                String             currStudyDate       = null;
                String             currStudyId         = null;
                String             currAccessionNumber = null;
                String             currPatientId       = null;
                String             currPatientName     = null;
                boolean            extraStudyInfoSet   = false;
                final List<String> seriesDescriptions  = studyInfo.getSeriesDescriptions();
                final List<String> seriesInstanceUIDs  = studyInfo.getSeriesInstanceUids();

                final Optional<String> currAnonScript = getAnonScript(studyInfo);
                if (currAnonScript.isPresent() && !importEvenIfCustomProcessingIsOff) {
                    validateDicomScpInstance(ae, aeTitle, port);
                }

                //TODO: We should just be able to uncomment the setStudyScript call and remove the 11 lines below it, but I'm having a build issue with the updated XNAT code not being picked up. This should be changed as soon as those issues are resolved.
                String login = _xnatUserProvider.getLogin();
                log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, currStudy);

                final PacsSearchResults<Series> series         = getSeriesByStudyUid(user, pacs, currStudy);
                final List<String>              seriesToImport = new ArrayList<>();
                final Collection<Series>        results        = series.getResults();
                if (CollectionUtils.isEmpty(seriesInstanceUIDs)) {
                    if (CollectionUtils.isEmpty(seriesDescriptions)) {
                        //Import all the series in the study
                        for (final Series currSeries : results) {
                            seriesToImport.add(currSeries.getSeriesInstanceUid());
                            if (!extraStudyInfoSet) {
                                currStudyDate = currSeries.getStudyDate();
                                currStudyId = currSeries.getStudyId();
                                currAccessionNumber = currSeries.getAccessionNumber();
                                currPatientId = currSeries.getPatientId();
                                currPatientName = currSeries.getPatientName();
                                extraStudyInfoSet = true;
                            }
                        }
                    } else {
                        //Import all the series in the study that have seriesDescription in the series description list
                        for (final Series currSeries : results) {
                            final String result = StringUtils.defaultIfBlank(currSeries.getSeriesInstanceUid(), "");
                            if (seriesDescriptions.contains(StringUtils.defaultIfBlank(currSeries.getSeriesDescription(), ""))) {
                                seriesToImport.add(result);
                                if (!extraStudyInfoSet) {
                                    currStudyDate = currSeries.getStudyDate();
                                    currStudyId = currSeries.getStudyId();
                                    currAccessionNumber = currSeries.getAccessionNumber();
                                    currPatientId = currSeries.getPatientId();
                                    currPatientName = currSeries.getPatientName();
                                    extraStudyInfoSet = true;
                                }
                            }
                        }
                    }
                } else {
                    if (CollectionUtils.isEmpty(seriesDescriptions)) {
                        //Import all the series in the study that are in the seriesUIDs list
                        for (final Series currSeries : results) {
                            final String result = StringUtils.defaultIfBlank(currSeries.getSeriesInstanceUid(), "");
                            if (seriesInstanceUIDs.contains(result)) {
                                seriesToImport.add(result);
                                if (!extraStudyInfoSet) {
                                    currStudyDate = currSeries.getStudyDate();
                                    currStudyId = currSeries.getStudyId();
                                    currAccessionNumber = currSeries.getAccessionNumber();
                                    currPatientId = currSeries.getPatientId();
                                    currPatientName = currSeries.getPatientName();
                                    extraStudyInfoSet = true;
                                }
                            }
                        }
                    } else {
                        //Import all the series in the study that are in the seriesUIDs list and have seriesDescription in the series description list
                        for (final Series currSeries : results) {
                            final String result = StringUtils.defaultIfBlank(currSeries.getSeriesInstanceUid(), "");
                            if (seriesDescriptions.contains(StringUtils.defaultIfBlank(currSeries.getSeriesDescription(), ""))) {
                                if (seriesInstanceUIDs.contains(result)) {
                                    seriesToImport.add(result);
                                    if (!extraStudyInfoSet) {
                                        currStudyDate = currSeries.getStudyDate();
                                        currStudyId = currSeries.getStudyId();
                                        currAccessionNumber = currSeries.getAccessionNumber();
                                        currPatientId = currSeries.getPatientId();
                                        currPatientName = currSeries.getPatientName();
                                        extraStudyInfoSet = true;
                                    }
                                }
                            }
                        }
                    }
                }

                if (!seriesToImport.isEmpty()) {
                    try {
                        final QueuedPacsRequest request = createQueuedPacsRequest(user, aeTitle, project, pacsId, multiStudy, currStudy, seriesToImport);
                        currAnonScript.ifPresent(request::setRemappingScript);
                        request.setStudyDate(currStudyDate);
                        request.setStudyId(currStudyId);
                        request.setAccessionNumber(currAccessionNumber);
                        request.setPatientId(currPatientId);
                        request.setPatientName(currPatientName);
                        _queuedPacsRequestService.create(request);
                        valueToReturn = false;
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
            }
        }
        return valueToReturn;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean processSpreadsheetImportFromRows(UserI user, List<CsvRow> rows, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception {
        Pacs pacs = _pacsEntityService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }
        String aeTitle = ae;
        String port    = "";
        if (ae != null && ae.contains(":")) {
            String[] parts = ae.split(":");
            aeTitle = parts[0];
            port = parts[1];
        }
        final AtomicBoolean      valueToReturn                 = new AtomicBoolean(true);
        final Map<Study, String> studiesListMappedToAnonScript = new HashMap<>();
        for (final CsvRow row : rows) {
            if (row != null && row.getStudies() != null) {
                for (final Study study : row.getStudies()) {
                    if (study != null && !studiesListMappedToAnonScript.containsKey(study)) {
                        final String anonScript = row.getAnonScript();
                        studiesListMappedToAnonScript.put(study, anonScript);
                        if (StringUtils.isNotBlank(anonScript) && !importEvenIfCustomProcessingIsOff) {
                            validateDicomScpInstance(ae, aeTitle, port);
                        }
                    }
                }
            }
        }
        final Set<Map.Entry<Study, String>> studiesSet = studiesListMappedToAnonScript.entrySet();
        final boolean                       multiStudy = studiesSet.size() > 1;
        for (final Map.Entry<Study, String> entry : studiesSet) {
            final Study  study      = entry.getKey();
            final String anonScript = entry.getValue();
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
                _queuedPacsRequestService.create(createQueuedPacsRequest(user, aeTitle, project, pacsId, multiStudy, studyId, getSeriesByStudy(user, pacs, study).getResults().stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList())));
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
    public void processSpreadsheetImport(final UserI user, final File csv, final String ae, final String project, final long pacsId) throws PacsNotFoundException {
        // TODO: The processSpreadsheetImport*() methods need refactoring similar to extract*ImportRequestFromCsv() methods to eliminate duplicate code
        final Pacs pacs = _pacsEntityService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }

        final Map<Study, String> studiesListMappedToAnonScript = new HashMap<>();
        try {
            final List<List<String>> rows                  = FileUtils.CSVFileToArrayList(csv);
            final List<String>       columnHeaders         = rows.get(0); //The first row must contain the column headers
            final int                accessionNumberColumn = columnHeaders.indexOf("Accession Number");
            final int                studyDateColumn       = columnHeaders.indexOf("Study Date");
            final int                patientIdColumn       = columnHeaders.indexOf("Patient ID");
            final int                lastNameColumn        = columnHeaders.indexOf("Last Name");
            final int                firstNameColumn       = columnHeaders.indexOf("First Name");
            final int                dobColumn             = columnHeaders.indexOf("DOB");
            final int                modalityColumn        = columnHeaders.indexOf("Modality");

            final Map<Integer, String> columnToDicomTagMap = new HashMap<>();
            for (final Map.Entry<String, String> entry : HEADER_TO_TAG_MAP.entrySet()) {
                final int indexOfHeader = columnHeaders.indexOf(entry.getKey());
                if (indexOfHeader != -1) {
                    columnToDicomTagMap.put(indexOfHeader, entry.getValue());
                }
            }

            rows.subList(1, rows.size()).forEach(row -> {
                final PacsSearchCriteria.PacsSearchCriteriaBuilder searchCriteriaBuilder = PacsSearchCriteria.builder();
                if (accessionNumberColumn != -1 && StringUtils.isNotBlank(row.get(accessionNumberColumn))) {
                    searchCriteriaBuilder.accessionNumber(row.get(accessionNumberColumn));
                }
                if ((lastNameColumn != -1 && StringUtils.isNotBlank(row.get(lastNameColumn))) || (firstNameColumn != -1 && StringUtils.isNotBlank(row.get(firstNameColumn)))) {
                    String lastName  = (lastNameColumn == -1 || StringUtils.isBlank(row.get(lastNameColumn))) ? "" : row.get(lastNameColumn);
                    String firstName = (firstNameColumn == -1 || StringUtils.isBlank(row.get(firstNameColumn))) ? "" : row.get(firstNameColumn);
                    searchCriteriaBuilder.patientName(StringUtils.isNotBlank(firstName) ? lastName + "," + firstName : lastName);
                }
                if (patientIdColumn != -1 && StringUtils.isNotBlank(row.get(patientIdColumn))) {
                    searchCriteriaBuilder.patientName(row.get(patientIdColumn));
                }
                if (studyDateColumn != -1 && StringUtils.isNotBlank(row.get(studyDateColumn))) {
                    final DqrDateRange dateRange = getDateRange(row.get(studyDateColumn));
                    if (dateRange != null) {
                        searchCriteriaBuilder.studyDateRange(dateRange);
                    }
                }
                if (dobColumn != -1 && StringUtils.isNotBlank(row.get(dobColumn))) {
                    searchCriteriaBuilder.dob(row.get(dobColumn));
                }
                if (modalityColumn != -1 && StringUtils.isNotBlank(row.get(modalityColumn))) {
                    searchCriteriaBuilder.modality(row.get(modalityColumn));
                }

                final Optional<String> script = getAnonScript(columnToDicomTagMap, row);
                try {
                    getStudiesByExample(user, pacs, searchCriteriaBuilder.build()).getResults().stream()
                                                                                  .filter(Objects::nonNull)
                                                                                  .filter(study -> !studiesListMappedToAnonScript.containsKey(study))
                                                                                  .forEach(study -> studiesListMappedToAnonScript.put(study, script.orElse(null)));
                } catch (PacsNotQueryableException e) {
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
            try {
                final QueuedPacsRequest request = createQueuedPacsRequest(user, ae, project, pacsId, multiStudy, studyInstanceUid, getSeriesByStudy(user, pacs, study).getResults().stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList()));
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

    @NotNull
    private QueuedPacsRequest createQueuedPacsRequest(final UserI user, final String ae, final String project, final long pacsId, final boolean multiStudy, final String studyId, final List<String> seriesIds) {
        return QueuedPacsRequest.builder()
                                .pacsId(pacsId)
                                .username(user.getUsername())
                                .xnatProject(project)
                                .studyInstanceUid(studyId)
                                .seriesIds(seriesIds)
                                .destinationAeTitle(ae)
                                .priority(multiStudy ? PacsRequest.STANDARD_PRIORITY : PacsRequest.HIGH_PRIORITY)
                                .status(PacsRequest.QUEUED_STATUS_TEXT)
                                .queuedTime(new Date()).build();
    }

    protected Study assignStudyToProject(final String projectId, final String studyInstanceUid, final String username) {
        if (!StringUtils.isBlank(projectId)) {
            if (log.isDebugEnabled()) {
                log.debug("Assigning study instance UID " + studyInstanceUid + " to project " + projectId);
            }
            _studyRoutingService.assign(studyInstanceUid, projectId, username);
            return new Study(projectId, studyInstanceUid);
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No project assignment specified for study instance UID " + studyInstanceUid + ", may be registered as Unassigned");
            }
            return new Study(studyInstanceUid);
        }
    }

    private interface RowProcessor<R> {
        R process(final UserI user, final Pacs pacs, final Map<Integer, String> columnMap, final List<String> row, final PacsSearchCriteria criteria);
    }

    private <R> List<R> _extractImportRequestFromCsv(final UserI user, final File csv, final long pacsId, final boolean allowRowThatGetsAllStudiesOnPacs, final boolean isNewRequest, final RowProcessor<R> processor) throws Exception {
        final Pacs pacs = _pacsEntityService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }

        final List<List<String>> rows                  = FileUtils.CSVFileToArrayList(csv);
        final List<String>       columnHeaders         = rows.get(0); //The first row must contain the column headers
        final int                accessionNumberColumn = columnHeaders.indexOf("Accession Number");
        final int                studyDateColumn       = columnHeaders.indexOf("Study Date");
        final int                patientIdColumn       = columnHeaders.indexOf("Patient ID");
        final int                lastNameColumn        = columnHeaders.indexOf("Last Name");
        final int                firstNameColumn       = columnHeaders.indexOf("First Name");
        final int                dobColumn             = columnHeaders.indexOf("DOB");
        final int                modalityColumn        = columnHeaders.indexOf("Modality");

        final Map<Integer, String> columnMap = new HashMap<>();
        for (final Map.Entry<String, String> entry : HEADER_TO_TAG_MAP.entrySet()) {
            final int indexOfHeader = columnHeaders.indexOf(entry.getKey());
            if (indexOfHeader != -1) {
                columnMap.put(indexOfHeader, isNewRequest ? entry.getKey() : entry.getValue());
            }
        }

        return rows.subList(1, rows.size()).stream().map(row -> {
            final AtomicBoolean areThereSearchCriteriaForThisRow = new AtomicBoolean();

            final PacsSearchCriteria.PacsSearchCriteriaBuilder searchCriteriaBuilder = PacsSearchCriteria.builder();
            if (accessionNumberColumn != -1 && StringUtils.isNotBlank(row.get(accessionNumberColumn))) {
                searchCriteriaBuilder.accessionNumber(isNewRequest ? removeExtraQuotes(row.get(accessionNumberColumn)) : row.get(accessionNumberColumn));
                areThereSearchCriteriaForThisRow.set(true);
            }
            if ((lastNameColumn != -1 && StringUtils.isNotBlank(row.get(lastNameColumn))) || (firstNameColumn != -1 && StringUtils.isNotBlank(row.get(firstNameColumn)))) {
                final String lastName  = (lastNameColumn == -1 || StringUtils.isBlank(row.get(lastNameColumn))) ? "" : row.get(lastNameColumn);
                final String firstName = (firstNameColumn == -1 || StringUtils.isBlank(row.get(firstNameColumn))) ? "" : row.get(firstNameColumn);
                final String fullName  = StringUtils.isNotBlank(firstName) ? lastName + "," + firstName : lastName;
                searchCriteriaBuilder.patientName(isNewRequest ? removeExtraQuotes(fullName) : fullName);
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (patientIdColumn != -1 && StringUtils.isNotBlank(row.get(patientIdColumn))) {
                searchCriteriaBuilder.patientId(isNewRequest ? removeExtraQuotes(row.get(patientIdColumn)) : row.get(patientIdColumn));
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (studyDateColumn != -1 && StringUtils.isNotBlank(row.get(studyDateColumn))) {
                final DqrDateRange dateRange = getDateRange(row.get(studyDateColumn));
                if (dateRange != null) {
                    searchCriteriaBuilder.studyDateRange(dateRange);
                    areThereSearchCriteriaForThisRow.set(true);
                }
            }
            if (dobColumn != -1 && StringUtils.isNotBlank(row.get(dobColumn))) {
                searchCriteriaBuilder.dob(isNewRequest ? removeExtraQuotes(row.get(dobColumn)) : row.get(dobColumn));
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (modalityColumn != -1 && StringUtils.isNotBlank(row.get(modalityColumn))) {
                searchCriteriaBuilder.modality(isNewRequest ? removeExtraQuotes(row.get(modalityColumn)) : row.get(modalityColumn));
                areThereSearchCriteriaForThisRow.set(true);
            }
            if (!areThereSearchCriteriaForThisRow.get() && !allowRowThatGetsAllStudiesOnPacs) {
                log.error("No search criteria found for row. Users must specify at least one valid search criteria.");
                return null;
            }

            return processor.process(user, pacs, columnMap, row, searchCriteriaBuilder.build());
        }).filter(Objects::nonNull).collect(Collectors.toList());
    }

    private CEchoSCU buildCEchoSCU(final Pacs pacs) {
        return new Dcm4cheToolCEchoSCU(_preferences, buildDicomConnectionProperties(pacs));
    }

    private CFindSCU buildCFindSCU(final Pacs pacs) throws PacsNotQueryableException {
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(pacs.getId());
        }
        return new Dcm4cheToolCFindSCU(_preferences, buildDicomConnectionProperties(pacs), getOrmStrategy(pacs));
    }

    @SuppressWarnings("unused")
    private CMoveSCU buildCMoveSCU(final Pacs pacs) {
        return new Dcm4cheToolCMoveSCU(_preferences, buildDicomConnectionProperties(pacs), getOrmStrategy(pacs));
    }

    private CStoreSCU buildCStoreSCU(final Pacs pacs) {
        return new BasicCStoreSCU(_preferences, buildDicomConnectionProperties(pacs));
    }

    private CMoveSCU buildCMoveSCU(final Pacs pacs, final String receiverAETitle) {
        return new Dcm4cheToolCMoveSCU(_preferences, buildDicomConnectionProperties(pacs, receiverAETitle), getOrmStrategy(pacs));
    }

    private void validateDicomScpInstance(final String ae, final String aeTitle, final String port) throws NotFoundException, UnknownDicomScpInstanceException, DicomReceiverCustomProcessingDisabledException, ArchiveProcessorsNotAvailableException {
        final DicomSCPInstance scpInstance = _dicomSCPManager.getDicomSCPInstance(aeTitle, Integer.parseInt(port));
        if (!scpInstance.isEnabled()) {
            throw new UnknownDicomScpInstanceException(aeTitle + ":" + port);
        }
        if (!scpInstance.isCustomProcessing()) {
            throw new DicomReceiverCustomProcessingDisabledException(scpInstance);
        }
        final List<ArchiveProcessorInstance> processorInstances = _archiveProcessorInstanceService.getAllEnabledSiteProcessorsForAe(ae);
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

    private static String removeExtraQuotes(String inputString) {
        if (inputString != null && StringUtils.equals(inputString, CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER)) {
            return CLEAR_SIGNIFIER;
        } else {
            return inputString;
        }
    }

    private static Map<String, String> createHeaderToTagMap() {
        return new HashMap<String, String>() {{
            put("Relabel Accession Number", "(0008,0050)");
            put("Relabel Study Date", "(0008,0020)");
            put("Relabel Study ID", "(0020,0010)");
            put("Relabel Patient ID", "(0010,0020)");
            put("Relabel Patient Name", "(0010,0010)");
            put("Relabel Patient Birth Date", "(0010,0030)");
            put("Subject", "(0010,0010):(0010,0020)");
            put("Session", "(0020,0010):(0008,0050)");
        }};
    }

    private static DqrDateRange getDateRange(final String studyDate) {
        if (!studyDate.contains("-")) {
            return new DqrDateRange(studyDate);
        }

        final String startDate = StringUtils.substringBefore(studyDate, "-");
        final String endDate   = StringUtils.substringAfter(studyDate, "-");

        // If all blank, range is open on both ends so no search criteria should be added.
        return StringUtils.isAllBlank(startDate, endDate) ? null : new DqrDateRange(startDate, endDate);
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
        if (map == null || map.isEmpty()) {
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

    private static final String              CLEAR_SIGNIFIER    = "\"\"";
    private static final String              CLEAR_SIGNIFIER_3X = CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER;
    private static final Map<String, String> HEADER_TO_TAG_MAP  = createHeaderToTagMap();

    private final DqrPreferences                                                             _preferences;
    private final DicomSCPManager                                                            _dicomSCPManager;
    private final QueuedPacsRequestService                                                   _queuedPacsRequestService;
    private final ConfigService                                                              _configService;
    private final StudyRoutingService                                                        _studyRoutingService;
    private final PacsEntityService                                                          _pacsEntityService;
    private final JmsTemplate                                                                _jmsTemplate;
    private final ArchiveProcessorInstanceService                                            _archiveProcessorInstanceService;
    private final XnatUserProvider                                                           _xnatUserProvider;
    private final Map<String, OrmStrategy>                                                   _ormStrategies;
    private final Map<UUID, Pair<PacsSearchRequest, Map<String, PacsSearchResults<Series>>>> _searchCache;
}
