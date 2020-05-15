/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.basic.BasicPacsService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.basic;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;
import javax.annotation.Nonnull;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jetbrains.annotations.NotNull;
import org.nrg.config.services.ConfigService;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.framework.constants.Scope;
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
import org.nrg.xnatx.dqr.exceptions.PacsNotFoundException;
import org.nrg.xnatx.dqr.exceptions.PacsNotQueryableException;
import org.nrg.xnatx.dqr.exceptions.PacsNotStorableException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.PacsEntityService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.nrg.xnatx.dqr.utils.CsvRow;
import org.nrg.xnatx.dqr.utils.DqrDateRange;
import org.nrg.xnatx.dqr.utils.DqrRuntimeException;
import org.nrg.xnatx.dqr.utils.FindRow;
import org.nrg.xnatx.dqr.utils.StudyImportInformation;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class BasicPacsService implements PacsService {
    @Autowired
    public BasicPacsService(final DqrPreferences preferences, final DicomSCPManager dicomSCPManager, final QueuedPacsRequestService queuedPacsRequestService, final ConfigService configService, final StudyRoutingService studyRoutingService, final PacsEntityService pacsEntityService, final ArchiveProcessorInstanceService archiveProcessorInstanceService, final XnatUserProvider primaryAdminUserProvider, final Map<String, OrmStrategy> ormStrategies) {
        _preferences = preferences;
        _dicomSCPManager = dicomSCPManager;
        _queuedPacsRequestService = queuedPacsRequestService;
        _configService = configService;
        _studyRoutingService = studyRoutingService;
        _pacsEntityService = pacsEntityService;
        _archiveProcessorInstanceService = archiveProcessorInstanceService;
        _xnatUserProvider = primaryAdminUserProvider;
        _ormStrategies = ormStrategies;
    }

    @Override
    public boolean canConnect(final UserI user, final Pacs pacs) {
        return buildCEchoSCU(pacs).canConnect();
    }

    @Override
    public PacsSearchResults<String, Patient> getPatientsByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria searchCriteria) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindPatientsByExample(searchCriteria);
    }

    @Override
    public Patient getPatientById(final UserI user, final Pacs pacs, final String patientId) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindPatientById(patientId);
    }

    @Override
    public PacsSearchResults<String, Study> getStudiesByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria searchCriteria) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindStudiesByExample(searchCriteria);
    }

    @Override
    public Study getStudyById(final UserI user, final Pacs pacs, final String studyInstanceUid) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindStudyById(studyInstanceUid);
    }

    @Override
    public PacsSearchResults<String, Series> getSeriesByStudy(final UserI user, final Pacs pacs, final Study study) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindSeriesByStudy(study);
    }

    @Override
    public PacsSearchResults<String, Series> getSeriesByStudyUid(final UserI user, final Pacs pacs, final String studyUid) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindSeriesByStudyUid(studyUid);
    }

    @Override
    public Series getSeriesById(final UserI user, final Pacs pacs, final String seriesInstanceUid) throws PacsNotQueryableException {
        return buildCFindSCU(pacs).cfindSeriesById(seriesInstanceUid);
    }

    @Override
    public void importSeries(final UserI user, final Pacs pacs, final Study study, final Series series, final String ae) {
        buildCMoveSCU(pacs, ae).cmoveSeries(study, series);
    }

    @Override
    public void importFromPacsRequest(final ExecutedPacsRequest request) throws PacsNotQueryableException, PacsNotStorableException {
        final Pacs pacs = _pacsEntityService.retrieve(request.getPacsId());
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(request.getPacsId());
        }
        final String aeAndPort = request.getDecodedAeAndPort();
        if (!aeIsStorable(aeAndPort)) {
            throw new PacsNotStorableException(aeAndPort);
        }
        final String aeTitle = StringUtils.substringBefore(aeAndPort, ":");
        try {
            final Study study = assignStudyToProject(request.getXnatProject(), request.getStudyInstanceUid(), request.getUsername());
            for (final String seriesId : request.getSeriesIds().split("\\s*,\\s*")) {
                log.debug("Requesting series {} for study instance UID {}", seriesId, request.getStudyInstanceUid());
                buildCMoveSCU(pacs, aeTitle).cmoveSeries(study, Series.builder().seriesInstanceUid(seriesId).build());
            }
        } catch (final CMoveTargetNotFoundException exception) {
            log.warn("C-MOVE target not found somehow: PACS {}", pacs, exception);
        }
    }

    @Override
    public void exportSeries(final UserI user, final Pacs pacs, final XnatImagescandata series) {
        buildCStoreSCU(pacs).cstoreSeries(series);
    }

    @Override
    public boolean aeIsStorable(final String ae) {
        //The user is able to store to an AE if there is either an XNAT SCP receiver with that AE or there is an enabled PACS with that AE for which storable=true
        final boolean hasPort = ae.contains(":");
        return _dicomSCPManager.getDicomSCPInstances().values().stream().anyMatch(scp -> scp.isEnabled() && StringUtils.equalsIgnoreCase(ae, hasPort ? scp.getAeTitle() + scp.getPort() : scp.getAeTitle()));
    }

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
                return FindRow.builder().criteria(criteria).relabelMap(anonMapForThisRow).studies(getStudiesByExample(user1, pacs, criteria).getResults().values()).build();
            } catch (PacsNotQueryableException e) {
                log.warn("The PACS {} is not queryable, returning null for search results", pacs.getLabel(), e);
                return null;
            }
        });
    }

    @Override
    public List<CsvRow> extractImportRequestFromCsv(final UserI user, final File csv, final long pacsId, final boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        return _extractImportRequestFromCsv(user, csv, pacsId, allowRowThatGetsAllStudiesOnPacs, false, (user1, pacs, columnMap, row, criteria) -> {
            final AtomicBoolean anonymizeThisRow     = new AtomicBoolean();
            final StringBuilder anonScriptForThisRow = new StringBuilder("version \"6.1\"" + System.lineSeparator());
            for (final Map.Entry<Integer, String> entry : columnMap.entrySet()) {
                final String stringToRemapTo = row.get(entry.getKey());
                if (StringUtils.isNotBlank(stringToRemapTo)) {
                    if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo) || StringUtils.equals(CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER, stringToRemapTo)) {
                        anonScriptForThisRow.append(entry.getValue()).append(" := \"\"").append(System.lineSeparator());
                    } else {
                        anonScriptForThisRow.append(entry.getValue()).append(" := \"").append(stringToRemapTo).append("\"").append(System.lineSeparator());
                    }
                    anonymizeThisRow.set(true);
                }
            }
            try {
                final PacsSearchResults<String, Study> results = getStudiesByExample(user1, pacs, criteria);
                return CsvRow.builder().criteria(criteria).anonScript(anonymizeThisRow.get() ? anonScriptForThisRow.toString() : null).studies(results.getResults().values()).build();
            } catch (PacsNotQueryableException e) {
                log.warn("The PACS {} is not queryable, returning null for search results", pacs.getLabel(), e);
                return null;
            }
        });
    }

    @Override
    public boolean processSpreadsheetImport(Map<String, StudyImportInformation> studiesToImport, UserI user, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception {
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
                String       currStudyDate          = null;
                String       currStudyId            = null;
                String       currAccessionNumber    = null;
                String       currPatientId          = null;
                String       currPatientName        = null;
                boolean      extraStudyInfoSet      = false;
                String       currAnonScript         = studyInfo.getAnonScript();
                List<String> seriesDescriptionsList = studyInfo.getSeriesDescriptions();
                List<String> seriesInstanceUIDs     = studyInfo.getSeriesInstanceUIDs();
                if (StringUtils.isBlank(currAnonScript)) {
                    Map<String, String> relabelMap = studyInfo.getRelabelMap();
                    if (relabelMap != null && relabelMap.size() > 0) {
                        currAnonScript = generateAnonScriptFromMap(relabelMap);
                    }
                }
                if (StringUtils.isNotBlank(currAnonScript) && !importEvenIfCustomProcessingIsOff) {
                    DicomSCPInstance scpInstance = _dicomSCPManager.getDicomSCPInstance(aeTitle, Integer.parseInt(port));
                    if (!scpInstance.isEnabled()) {
                        throw new Exception("Invalid DICOM SCP Receiver ID.");
                    }
                    if (!scpInstance.isCustomProcessing()) {
                        throw new Exception("You are trying to remap DICOM fields. For this to work, custom processing must be enabled for this SCP receiver.");
                    }
                    final List<ArchiveProcessorInstance> processorInstances = _archiveProcessorInstanceService.getAllEnabledSiteProcessorsForAe(ae);
                    if (processorInstances.isEmpty() || processorInstances.stream().allMatch(instance -> StringUtils.equals(instance.getProcessorClass(), "org.nrg.xnat.processors.MizerArchiveProcessor"))) {
                        throw new Exception("You are trying to remap DICOM fields. For this to work, you must have a remapping processor for this SCP receiver.");
                    }
                }

                //TODO: We should just be able to uncomment the setStudyScript call and remove the 11 lines below it, but I'm having a build issue with the updated XNAT code not being picked up. This should be changed as soon as those issues are resolved.
                String login = _xnatUserProvider.getLogin();
                log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, currStudy);

                final PacsSearchResults<String, Series> series         = getSeriesByStudyUid(user, pacs, currStudy);
                List<String>                            seriesToImport = new ArrayList<>();
                final Collection<Series>                results        = series.getResults().values();
                if (CollectionUtils.isEmpty(seriesInstanceUIDs)) {
                    if (CollectionUtils.isEmpty(seriesDescriptionsList)) {
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
                            String result = currSeries.getSeriesInstanceUid();
                            if (seriesDescriptionsList.contains(currSeries.getSeriesDescription()) || (currSeries.getSeriesDescription() == null && seriesDescriptionsList.contains(""))) {
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
                    if (CollectionUtils.isEmpty(seriesDescriptionsList)) {
                        //Import all the series in the study that are in the seriesUIDs list
                        for (final Series currSeries : results) {
                            String result = currSeries.getSeriesInstanceUid();
                            if (seriesInstanceUIDs.contains(result) || (result == null && seriesInstanceUIDs.contains(""))) {
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
                            String result = currSeries.getSeriesInstanceUid();
                            if (seriesDescriptionsList.contains(currSeries.getSeriesDescription()) || (currSeries.getSeriesDescription() == null && seriesDescriptionsList.contains(""))) {
                                if (seriesInstanceUIDs.contains(result) || (result == null && seriesInstanceUIDs.contains(""))) {
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

                final String seriesIdsString = StringUtils.join(seriesToImport, ",");
                if (StringUtils.isNotBlank(seriesIdsString)) {
                    try {
                        final QueuedPacsRequest request = createQueuedPacsRequest(user, aeTitle, project, pacsId, multiStudy, currStudy, seriesIdsString);
                        if (currAnonScript != null) {
                            request.setRemappingScript(currAnonScript);
                        }
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
                            final DicomSCPInstance scpInstance = _dicomSCPManager.getDicomSCPInstance(aeTitle, Integer.parseInt(port));
                            if (!scpInstance.isEnabled()) {
                                throw new Exception("Invalid DICOM SCP Receiver ID.");
                            }
                            if (!scpInstance.isCustomProcessing()) {
                                throw new Exception("You are trying to remap DICOM fields. For this to work, custom processing must be enabled for this SCP receiver.");
                            }
                            final List<ArchiveProcessorInstance> processorInstances = _archiveProcessorInstanceService.getAllEnabledSiteProcessorsForAe(ae);
                            if (processorInstances.isEmpty()) {
                                throw new Exception("You are trying to remap DICOM fields. For this to work, you must have a remapping processor for this SCP receiver.");
                            }
                            if (processorInstances.stream().allMatch(instance -> StringUtils.equals(instance.getProcessorClass(), "org.nrg.xnat.processors.MizerArchiveProcessor"))) {
                                throw new Exception("You are trying to remap DICOM fields. For this to work, you must have a remapping processor for this SCP receiver.");
                            }
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

            final String seriesIds = StringUtils.join(getSeriesByStudy(user, pacs, study).getResults().values().stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList()), ",");

            try {
                _queuedPacsRequestService.create(createQueuedPacsRequest(user, aeTitle, project, pacsId, multiStudy, studyId, seriesIds));
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

                final AtomicBoolean anonymizeThisRow     = new AtomicBoolean();
                final StringBuilder anonScriptForThisRow = new StringBuilder("version \"6.1\"" + System.lineSeparator());
                for (final Map.Entry<Integer, String> entry : columnToDicomTagMap.entrySet()) {
                    final String stringToRemapTo = row.get(entry.getKey());
                    if (StringUtils.isNotBlank(stringToRemapTo)) {
                        if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo) || StringUtils.equals(CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER, stringToRemapTo)) {
                            anonScriptForThisRow.append(entry.getValue()).append(" := \"\"").append(System.lineSeparator());
                        } else {
                            anonScriptForThisRow.append(entry.getValue()).append(" := \"").append(stringToRemapTo).append("\"").append(System.lineSeparator());
                        }
                        anonymizeThisRow.set(true);
                    }
                }

                try {
                    getStudiesByExample(user, pacs, searchCriteriaBuilder.build()).getResults().values().stream()
                                                                                  .filter(Objects::nonNull)
                                                                                  .filter(study -> !studiesListMappedToAnonScript.containsKey(study))
                                                                                  .forEach(study -> studiesListMappedToAnonScript.put(study, anonymizeThisRow.get() ? anonScriptForThisRow.toString() : null));
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

            try {
                final String                            studyId   = study.getStudyInstanceUid();
                final PacsSearchResults<String, Series> series    = getSeriesByStudy(user, pacs, study);
                final String                            seriesIds = StringUtils.join(series.getResults().values().stream().map(Series::getSeriesInstanceUid).collect(Collectors.toList()), ",");
                final QueuedPacsRequest                 request   = createQueuedPacsRequest(user, ae, project, pacsId, multiStudy, studyId, seriesIds);
                request.setRemappingScript(anonScript);
                _queuedPacsRequestService.create(request);
            } catch (PacsNotQueryableException e) {
                log.warn("The PACS {} is not queryable, request not created for the study {}", pacs.getLabel(), study.getStudyInstanceUid(), e);
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
    private QueuedPacsRequest createQueuedPacsRequest(final UserI user, final String ae, final String project, final long pacsId, final boolean multiStudy, final String studyId, final String seriesIds) {
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
//            if (isNewRequest) {
//                final Map<String, String> anonMapForThisRow = new HashMap<>();
//                for (final Map.Entry<Integer, String> entry : columnMap.entrySet()) {
//                    final String stringToRemapTo = row.get(entry.getKey());
//                    if (StringUtils.isNotBlank(stringToRemapTo)) {
//                        if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo) || StringUtils.equals(CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER, stringToRemapTo)) {
//                            anonMapForThisRow.put(entry.getValue(), "\"\"");
//                        } else {
//                            anonMapForThisRow.put(entry.getValue(), stringToRemapTo);
//                        }
//                    }
//                }
//
//                try {
//                    final PacsSearchCriteria searchCriteria = searchCriteriaBuilder.build();
//                    return FindRow.builder().criteria(searchCriteria).relabelMap(anonMapForThisRow).studies(getStudiesByExample(user, pacs, searchCriteria).getResults().values()).build();
//                } catch (PacsNotQueryableException e) {
//                    log.warn("The PACS {} is not queryable, returning null for search results", pacs.getLabel(), e);
//                    return null;
//                }
//            } else {
//                final AtomicBoolean anonymizeThisRow     = new AtomicBoolean();
//                final StringBuilder anonScriptForThisRow = new StringBuilder("version \"6.1\"" + System.lineSeparator());
//                for (final Map.Entry<Integer, String> entry : columnMap.entrySet()) {
//                    final String stringToRemapTo = row.get(entry.getKey());
//                    if (StringUtils.isNotBlank(stringToRemapTo)) {
//                        if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo) || StringUtils.equals(CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER, stringToRemapTo)) {
//                            anonScriptForThisRow.append(entry.getValue()).append(" := \"\"").append(System.lineSeparator());
//                        } else {
//                            anonScriptForThisRow.append(entry.getValue()).append(" := \"").append(stringToRemapTo).append("\"").append(System.lineSeparator());
//                        }
//                        anonymizeThisRow.set(true);
//                    }
//                }
//                try {
//                    final PacsSearchCriteria               searchCriteria = searchCriteriaBuilder.build();
//                    final PacsSearchResults<String, Study> results        = getStudiesByExample(user, pacs, searchCriteria);
//                    return CsvRow.builder().criteria(searchCriteria).anonScript(anonymizeThisRow.get() ? anonScriptForThisRow.toString() : null).studies(results.getResults().values()).build();
//                } catch (PacsNotQueryableException e) {
//                    log.warn("The PACS {} is not queryable, returning null for search results", pacs.getLabel(), e);
//                    return null;
//                }
//            }
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

    private static String generateAnonScriptFromMap(@Nonnull final Map<String, String> relabelMap) {
        if (relabelMap.isEmpty()) {
            return null;
        }
        final StringBuilder currAnonScript = new StringBuilder("version \"6.1\"" + System.lineSeparator());
        for (final Map.Entry<String, String> entry : relabelMap.entrySet()) {
            final String[] tags     = StringUtils.split(HEADER_TO_TAG_MAP.get(entry.getKey()), ":");
            final String   newValue = entry.getValue();
            if (StringUtils.isNotBlank(newValue) && tags != null) {
                for (final String tag : tags) {
                    if (StringUtils.equals(CLEAR_SIGNIFIER, newValue) || StringUtils.equals(CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER, newValue)) {
                        currAnonScript.append(tag).append(" := \"\"").append(System.lineSeparator());
                    } else {
                        currAnonScript.append(tag).append(" := \"").append(newValue).append("\"").append(System.lineSeparator());
                    }
                }
            }
        }
        return currAnonScript.toString();
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

    private static final String              CLEAR_SIGNIFIER   = "\"\"";
    private static final Map<String, String> HEADER_TO_TAG_MAP = createHeaderToTagMap();

    private final DqrPreferences                  _preferences;
    private final DicomSCPManager                 _dicomSCPManager;
    private final QueuedPacsRequestService        _queuedPacsRequestService;
    private final ConfigService                   _configService;
    private final StudyRoutingService             _studyRoutingService;
    private final PacsEntityService               _pacsEntityService;
    private final ArchiveProcessorInstanceService _archiveProcessorInstanceService;
    private final XnatUserProvider                _xnatUserProvider;
    private final Map<String, OrmStrategy>        _ormStrategies;
}
