/*
 * org.nrg.dqr.services.BasicPacsService
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.services;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.services.ConfigService;
import org.nrg.dcm.scp.DicomSCPInstance;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.dqr.dicom.command.cecho.CEchoSCU;
import org.nrg.dqr.dicom.command.cecho.dcm4che.tool.Dcm4cheToolCEchoSCU;
import org.nrg.dqr.dicom.command.cfind.CFindSCU;
import org.nrg.dqr.dicom.command.cfind.dcm4che.tool.Dcm4cheToolCFindSCU;
import org.nrg.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.dqr.dicom.command.cmove.CMoveSCU;
import org.nrg.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.dqr.dicom.command.cmove.dcm4che.tool.Dcm4cheToolCMoveSCU;
import org.nrg.dqr.dicom.command.cstore.BasicCStoreSCU;
import org.nrg.dqr.dicom.command.cstore.CStoreSCU;
import org.nrg.dqr.dicom.net.DicomConnectionProperties;
import org.nrg.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.dqr.domain.Patient;
import org.nrg.dqr.domain.Series;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.restlet.NullValueSerializer;
import org.nrg.dqr.util.CsvRow;
import org.nrg.dqr.util.DqrRuntimeException;
import org.nrg.dqr.util.FindRow;
import org.nrg.dqr.util.StudyImportInformation;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.entities.ArchiveProcessorInstance;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.processor.services.ArchiveProcessorInstanceService;
import org.nrg.xnat.restlet.extensions.PacsNotFoundException;
import org.nrg.xnat.restlet.extensions.PacsNotQueryableException;
import org.nrg.xnat.restlet.extensions.PacsNotStorableException;
import org.nrg.xnat.utils.DqrDateRange;
import org.nrg.xnat.utils.MethodName;
import org.nrg.xnat.utils.WorkflowUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.*;

@Service
@Slf4j
public class BasicPacsService implements PacsService {
    private final PacsEntityService               _pacsEntityService;
    private final ArchiveProcessorInstanceService _archiveProcessorInstanceService;
    private final SiteConfigPreferences           _siteConfigPreferences;
    private final DicomSCPManager                 _dicomSCPManager;
    private final Map<String, OrmStrategy>        _strategies;
    private final QueuedPacsRequestService        _queuedPacsRequestService;
    private final ConfigService                   _configService;
    private final StudyRoutingService             _studyRoutingService;
    private final XnatUserProvider                _userProvider;

    @Autowired
    public BasicPacsService(final PacsEntityService pacsEntityService, final ArchiveProcessorInstanceService archiveProcessorInstanceService, final QueuedPacsRequestService queuedPacsRequestService, final StudyRoutingService studyRoutingService, final DqrPreferences preferences, final SiteConfigPreferences siteConfigPreferences, final DicomSCPManager dicomSCPManager, final ConfigService configService, final Map<String, OrmStrategy> strategies, final XnatUserProvider primaryAdminUserProvider) {
        _pacsEntityService = pacsEntityService;
        _archiveProcessorInstanceService = archiveProcessorInstanceService;
        _queuedPacsRequestService = queuedPacsRequestService;
        _studyRoutingService = studyRoutingService;
        _configService = configService;
        _preferences = preferences;
        _siteConfigPreferences = siteConfigPreferences;
        _dicomSCPManager = dicomSCPManager;
        _strategies = strategies;
        _userProvider = primaryAdminUserProvider;
    }

    @Override
    public boolean canConnect(final UserI user, final Pacs pacs) {
        try {
            return buildCEchoSCU(pacs).canConnect();
        } catch (Throwable ignored) {
        }
        return false;
    }

    @Override
    public PacsSearchResults<String, Patient> getPatientsByExample(final UserI user, final Pacs pacs, final PacsSearchCriteria searchCriteria) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(user, "pacs:query", null, null, EventUtils.newEventInstance(
                    EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE, MethodName.currentMethodName(), null,
                    MAPPER.writeValueAsString(searchCriteria)));
            final PacsSearchResults<String, Patient> results = buildCFindSCU(pacs).cfindPatientsByExample(
                    searchCriteria);
            completeWorkflow(workflow);
            return results;
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Patient getPatientById(final UserI user, final Pacs pacs, final String patientId) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(
                    user,
                    "pacs:query",
                    pacs.getAeTitle(),
                    null,
                    EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE,
                                                MethodName.currentMethodName(), null, MAPPER.writeValueAsString(patientId)));
            final Patient results = buildCFindSCU(pacs).cfindPatientById(patientId);
            completeWorkflow(workflow);
            return results;
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
        }
    }

    @Override
    public PacsSearchResults<String, Study> getStudiesByExample(final UserI user, final Pacs pacs,
                                                                final PacsSearchCriteria searchCriteria) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(user, "pacs:query", pacs.getAeTitle(), null, EventUtils.newEventInstance(
                    EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE, MethodName.currentMethodName(), null,
                    MAPPER.writeValueAsString(searchCriteria)));
            final PacsSearchResults<String, Study> results = buildCFindSCU(pacs).cfindStudiesByExample(searchCriteria);
            completeWorkflow(workflow);
            return results;
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Study getStudyById(final UserI user, final Pacs pacs, final String studyInstanceUid) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(
                    user,
                    "pacs:query",
                    pacs.getAeTitle(),
                    null,
                    EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE,
                                                MethodName.currentMethodName(), null,
                                                MAPPER.writeValueAsString(studyInstanceUid)));
            final Study results = buildCFindSCU(pacs).cfindStudyById(studyInstanceUid);
            completeWorkflow(workflow);
            return results;
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
        }
    }

    @Override
    public PacsSearchResults<String, Series> getSeriesByStudy(final UserI user, final Pacs pacs, final Study study) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(
                    user,
                    "pacs:query",
                    pacs.getAeTitle(),
                    null,
                    EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE,
                                                MethodName.currentMethodName(), null, MAPPER.writeValueAsString(study)));
            final PacsSearchResults<String, Series> results = buildCFindSCU(pacs).cfindSeriesByStudy(study);
            completeWorkflow(workflow);
            return results;
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
        }
    }

    @Override
    public PacsSearchResults<String, Series> getSeriesByStudyUid(final UserI user, final Pacs pacs, final String studyUid) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(
                    user,
                    "pacs:query",
                    pacs.getAeTitle(),
                    null,
                    EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE,
                                                MethodName.currentMethodName(), null, studyUid));
            final PacsSearchResults<String, Series> results = buildCFindSCU(pacs).cfindSeriesByStudyUid(studyUid);
            completeWorkflow(workflow);
            return results;
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
        }
    }

    @Override
    public Series getSeriesById(final UserI user, final Pacs pacs, final String seriesInstanceUid) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(
                    user,
                    "pacs:query",
                    pacs.getAeTitle(),
                    null,
                    EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE,
                                                MethodName.currentMethodName(), null,
                                                MAPPER.writeValueAsString(seriesInstanceUid)));
            final Series results = buildCFindSCU(pacs).cfindSeriesById(seriesInstanceUid);
            completeWorkflow(workflow);
            return results;
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void importSeries(final UserI user, final Pacs pacs, final Study study, final Series series, final String ae) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(
                    user,
                    "pacs:import",
                    pacs.getAeTitle(),
                    null,
                    EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE,
                                                MethodName.currentMethodName(), null, MAPPER.writeValueAsString(series)));
            buildCMoveSCU(pacs, ae).cmoveSeries(study, series);
            completeWorkflow(workflow);
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void importFromPacsRequest(final ExecutedPacsRequest request) throws PacsNotQueryableException, PacsNotStorableException {
        Pacs   pacs                 = _pacsEntityService.retrieve(request.getPacsId());
        String destinationAeAndPort = request.getDestinationAeTitle();

        try {
            destinationAeAndPort = URLDecoder.decode(destinationAeAndPort, "UTF-8");
        } catch (Exception e) {
        }

        String destinationAe = destinationAeAndPort;
        if (destinationAe != null && destinationAe.contains(":")) {
            String[] parts = destinationAe.split(":");
            destinationAe = parts[0];
        }
        if (!pacs.isQueryable()) {
            throw new PacsNotQueryableException(pacs.getId());
        } else if (!aeIsStorable(destinationAeAndPort)) {
            throw new PacsNotStorableException(pacs.getId());
        } else {
            try {

                final Study study = assignStudyToProject(request.getXnatProject(), request.getStudyInstanceUid(), request.getUsername());

                for (final String seriesId : request.getSeriesIds().split("\\s*,\\s*")) {
                    log.debug("Requesting series {} for study instance UID {}", seriesId, request.getStudyInstanceUid());
                    final Series series = new Series(seriesId);

                    PersistentWorkflowI workflow = null;
                    try {
                        workflow = buildOpenWorkflow(
                                new XDATUser(request.getUsername()),
                                "pacs:import",
                                pacs.getAeTitle(),
                                null,
                                EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE,
                                                            MethodName.currentMethodName(), null, MAPPER.writeValueAsString(series)));
                        buildCMoveSCU(pacs, destinationAe).cmoveSeries(study, series);
                        completeWorkflow(workflow);
                    } catch (final Exception e) {
                        failWorkflow(workflow);
                        throw new RuntimeException(e);
                    }
                }
            } catch (final CMoveTargetNotFoundException exception) {
                log.warn("C-MOVE target not found somehow: PACS [ aeTitle: " + pacs.getAeTitle() + ", ", exception);
            }
        }
    }

    @Override
    public void exportSeries(final UserI user, final Pacs pacs, final XnatImagescandata series) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(
                    user,
                    series.getXSIType(),
                    series.getId(),
                    series.getProject(),
                    EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE,
                                                MethodName.currentMethodName(), null, series.getId()));
            buildCStoreSCU(pacs).cstoreSeries(series);
            completeWorkflow(workflow);
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
        }
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
        return new DicomConnectionProperties(_dicomSCPManager.getDicomSCPInstances().values().iterator().next().getAeTitle(), pacs);
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
        try {
            if (_strategies.containsKey(beanId)) {
                return _strategies.get(beanId);
            }
        } catch (final Exception e) {
            throw new DqrRuntimeException(String.format("Failed to load the ORM strategy defined by bean '%s'", beanId), e);
        }
        throw new DqrRuntimeException(String.format("Failed to load the ORM strategy defined by bean '%s'", beanId));
    }

    private PersistentWorkflowI buildOpenWorkflow(final UserI user, final String xsiType, final String ID,
                                                  final String project_id, final EventDetails event) {
        if (!leaveAuditTrail()) {
            return null;
        } else {
            try {
                return WorkflowUtils.buildOpenWorkflow(user, xsiType, ID, project_id, event);
            } catch (final Exception e) {
                throw new RuntimeException(e);
            }
        }
    }

    private void completeWorkflow(final PersistentWorkflowI workflow) {
        try {
            if (leaveAuditTrail() && workflow != null) {
                WorkflowUtils.complete(workflow, workflow.buildEvent());
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private void failWorkflow(final PersistentWorkflowI workflow) {
        try {
            if (leaveAuditTrail() && workflow != null) {
                WorkflowUtils.fail(workflow, workflow.buildEvent());
            }
        } catch (final Exception e) {
            throw new RuntimeException(e);
        }
    }

    private boolean leaveAuditTrail() {
        return ObjectUtils.defaultIfNull(_siteConfigPreferences.getBooleanValue("leavePacsAuditTrail"), false);
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();

    static {
        final DefaultSerializerProvider provider = new DefaultSerializerProvider.Impl();
        provider.setNullValueSerializer(new NullValueSerializer());
        MAPPER.setSerializerProvider(provider);
    }

    @Override
    public boolean aeIsStorable(final String ae) {
        //The user is able to store to an AE if there is either an XNAT SCP receiver with that AE or there is an enabled PACS with that AE for which storable=true
        final boolean hasPort = ae.contains(":");
        for (final DicomSCPInstance scp : _dicomSCPManager.getDicomSCPInstances().values()) {
            if (((hasPort && StringUtils.equals(scp.getAeTitle() + ":" + scp.getPort(), ae)) || (!hasPort && StringUtils.equals(scp.getAeTitle(), ae))) && scp.isEnabled()) {
                return true;
            }
        }
//
//        final List<Pacs> allPacs = _pacsEntityService.findAllStorable();
//        for (Pacs pacsToCheck : allPacs){
//            if(StringUtils.equals(pacsToCheck.getAeTitle(),ae)){
//                return true;
//            }
//        }
        return false;
    }

    @Override
    public List<CsvRow> extractImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        final Pacs pacs = _pacsEntityService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }
        ArrayList<CsvRow> resultRows = new ArrayList<>();

        List<List<String>> rows                  = FileUtils.CSVFileToArrayList(csv);
        List<String>       columnHeaders         = rows.get(0); //The first row must contain the column headers
        int                accessionNumberColumn = columnHeaders.indexOf("Accession Number");
        int                studyDateColumn       = columnHeaders.indexOf("Study Date");
        int                patientIdColumn       = columnHeaders.indexOf("Patient ID");
        int                lastNameColumn        = columnHeaders.indexOf("Last Name");
        int                firstNameColumn       = columnHeaders.indexOf("First Name");
        int                dobColumn             = columnHeaders.indexOf("DOB");
        int                modalityColumn        = columnHeaders.indexOf("Modality");

        HashMap<Integer, String> columnToDicomTagMap = new HashMap<>();
        for (Map.Entry<String, String> entry : HEADER_TO_TAG_MAP.entrySet()) {
            int indexOfHeader = columnHeaders.indexOf(entry.getKey());
            if (indexOfHeader != -1) {
                columnToDicomTagMap.put(indexOfHeader, entry.getValue());
            }
        }

        for (int index = 1; index < rows.size(); index++) {//Skip the first row since that is a header row
            List<String> row                              = rows.get(index);
            boolean      areThereSearchCriteriaForThisRow = false;

            final PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
            if (accessionNumberColumn != -1 && StringUtils.isNotBlank(row.get(accessionNumberColumn))) {
                searchCriteria.setAccessionNumber(row.get(accessionNumberColumn));
                areThereSearchCriteriaForThisRow = true;
            }
            if ((lastNameColumn != -1 && StringUtils.isNotBlank(row.get(lastNameColumn))) || (firstNameColumn != -1 && StringUtils.isNotBlank(row.get(firstNameColumn)))) {
                String lastName  = (lastNameColumn == -1 || StringUtils.isBlank(row.get(lastNameColumn))) ? "" : row.get(lastNameColumn);
                String firstName = (firstNameColumn == -1 || StringUtils.isBlank(row.get(firstNameColumn))) ? "" : row.get(firstNameColumn);
                if (StringUtils.isNotBlank(firstName)) {
                    searchCriteria.setPatientName(lastName + "," + firstName);
                } else {
                    searchCriteria.setPatientName(lastName);
                }
                areThereSearchCriteriaForThisRow = true;
            }
            if (patientIdColumn != -1 && StringUtils.isNotBlank(row.get(patientIdColumn))) {
                searchCriteria.setPatientId(row.get(patientIdColumn));
                areThereSearchCriteriaForThisRow = true;
            }
            if (studyDateColumn != -1 && StringUtils.isNotBlank(row.get(studyDateColumn))) {
                String           studyDateCell = row.get(studyDateColumn);
                SimpleDateFormat formatter     = new SimpleDateFormat("yyyyMMdd");
                int              dashIndex     = studyDateCell.indexOf("-");
                if (dashIndex == -1) {
                    Date     dateObject = formatter.parse(studyDateCell);
                    Calendar c          = Calendar.getInstance();
                    c.setTime(dateObject);
                    c.add(Calendar.DATE, 1);
                    Date endOfDay = c.getTime();

                    searchCriteria.setStudyDateRange(new DqrDateRange(dateObject, endOfDay));
                    areThereSearchCriteriaForThisRow = true;
                } else {
                    String startDateString = studyDateCell.substring(0, dashIndex);
                    String endDateString   = studyDateCell.substring(dashIndex + 1);
                    if (StringUtils.isNotBlank(startDateString)) {
                        if (StringUtils.isNotBlank(endDateString)) {
                            searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), formatter.parse(endDateString)));
                            areThereSearchCriteriaForThisRow = true;
                        } else {
                            searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), null));
                            areThereSearchCriteriaForThisRow = true;
                        }
                    } else {
                        if (StringUtils.isNotBlank(endDateString)) {
                            searchCriteria.setStudyDateRange(new DqrDateRange(null, formatter.parse(endDateString)));
                            areThereSearchCriteriaForThisRow = true;
                        } else {
                            //Range is open ended on both ends so no search criteria should be added.
                        }
                    }
                }
            }
            if (dobColumn != -1 && StringUtils.isNotBlank(row.get(dobColumn))) {
                searchCriteria.setDob(row.get(dobColumn));
                areThereSearchCriteriaForThisRow = true;
            }
            if (modalityColumn != -1 && StringUtils.isNotBlank(row.get(modalityColumn))) {
                searchCriteria.setModality(row.get(modalityColumn));
                areThereSearchCriteriaForThisRow = true;
            }
            if (!areThereSearchCriteriaForThisRow && !allowRowThatGetsAllStudiesOnPacs) {
                throw new Exception("No search criteria found. Users must specify at least one valid search criteria.");
            }

            final PacsSearchResults<String, Study> studies = getStudiesByExample(user, pacs, searchCriteria);

            boolean             anonymizeThisRow     = false;
            final StringBuilder anonScriptForThisRow = new StringBuilder("version \"6.1\"" + System.lineSeparator());
            for (Map.Entry<Integer, String> entry : columnToDicomTagMap.entrySet()) {
                String stringToRemapTo = row.get(entry.getKey());
                if (StringUtils.isNotBlank(stringToRemapTo)) {
//                        if (StringUtils.equals(DELETE_SIGNIFIER,stringToRemapTo)) {
//                            anonScriptForThisRow += "- " + entry.getValue() + System.lineSeparator();
//                            anonymizeThisRow = true;
//                        } else if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo)) {
                    if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo) || StringUtils.equals(CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER, stringToRemapTo)) {
                        anonScriptForThisRow.append(entry.getValue()).append(" := \"\"").append(System.lineSeparator());
                        anonymizeThisRow = true;
                    } else {
                        anonScriptForThisRow.append(entry.getValue()).append(" := \"").append(stringToRemapTo).append("\"").append(System.lineSeparator());
                        anonymizeThisRow = true;
                    }
                }
            }
            resultRows.add(new CsvRow(searchCriteria, anonymizeThisRow ? anonScriptForThisRow.toString() : null, new ArrayList<>(studies.getResults())));
        }
        return resultRows;
    }

    @Override
    public List<FindRow> extractNewImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        final Pacs pacs = _pacsEntityService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }
        ArrayList<FindRow> resultRows = new ArrayList<>();

        List<List<String>> rows                  = FileUtils.CSVFileToArrayList(csv);
        List<String>       columnHeaders         = rows.get(0); //The first row must contain the column headers
        int                accessionNumberColumn = columnHeaders.indexOf("Accession Number");
        int                studyDateColumn       = columnHeaders.indexOf("Study Date");
        int                patientIdColumn       = columnHeaders.indexOf("Patient ID");
        int                lastNameColumn        = columnHeaders.indexOf("Last Name");
        int                firstNameColumn       = columnHeaders.indexOf("First Name");
        int                dobColumn             = columnHeaders.indexOf("DOB");
        int                modalityColumn        = columnHeaders.indexOf("Modality");

        HashMap<Integer, String> columnToColumnHeaderMap = new HashMap<>();
        for (Map.Entry<String, String> entry : HEADER_TO_TAG_MAP.entrySet()) {
            int indexOfHeader = columnHeaders.indexOf(entry.getKey());
            if (indexOfHeader != -1) {
                columnToColumnHeaderMap.put(indexOfHeader, entry.getKey());
            }
        }

        for (int index = 1; index < rows.size(); index++) {//Skip the first row since that is a header row
            List<String> row                              = rows.get(index);
            boolean      areThereSearchCriteriaForThisRow = false;

            final PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
            if (accessionNumberColumn != -1 && StringUtils.isNotBlank(row.get(accessionNumberColumn))) {
                searchCriteria.setAccessionNumber(removeExtraQuotes(row.get(accessionNumberColumn)));
                areThereSearchCriteriaForThisRow = true;
            }
            if ((lastNameColumn != -1 && StringUtils.isNotBlank(row.get(lastNameColumn))) || (firstNameColumn != -1 && StringUtils.isNotBlank(row.get(firstNameColumn)))) {
                String lastName  = (lastNameColumn == -1 || StringUtils.isBlank(row.get(lastNameColumn))) ? "" : row.get(lastNameColumn);
                String firstName = (firstNameColumn == -1 || StringUtils.isBlank(row.get(firstNameColumn))) ? "" : row.get(firstNameColumn);
                if (StringUtils.isNotBlank(firstName)) {
                    searchCriteria.setPatientName(removeExtraQuotes(lastName + "," + firstName));
                } else {
                    searchCriteria.setPatientName(removeExtraQuotes(lastName));
                }
                areThereSearchCriteriaForThisRow = true;
            }
            if (patientIdColumn != -1 && StringUtils.isNotBlank(row.get(patientIdColumn))) {
                searchCriteria.setPatientId(removeExtraQuotes(row.get(patientIdColumn)));
                areThereSearchCriteriaForThisRow = true;
            }
            if (studyDateColumn != -1 && StringUtils.isNotBlank(row.get(studyDateColumn))) {
                String           studyDateCell = row.get(studyDateColumn);
                SimpleDateFormat formatter     = new SimpleDateFormat("yyyyMMdd");
                int              dashIndex     = studyDateCell.indexOf("-");
                if (dashIndex == -1) {
                    Date     dateObject = formatter.parse(studyDateCell);
                    Calendar c          = Calendar.getInstance();
                    c.setTime(dateObject);
                    c.add(Calendar.DATE, 1);
                    Date endOfDay = c.getTime();

                    searchCriteria.setStudyDateRange(new DqrDateRange(dateObject, endOfDay));
                    areThereSearchCriteriaForThisRow = true;
                } else {
                    String startDateString = studyDateCell.substring(0, dashIndex);
                    String endDateString   = studyDateCell.substring(dashIndex + 1);
                    if (StringUtils.isNotBlank(startDateString)) {
                        if (StringUtils.isNotBlank(endDateString)) {
                            searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), formatter.parse(endDateString)));
                            areThereSearchCriteriaForThisRow = true;
                        } else {
                            searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), null));
                            areThereSearchCriteriaForThisRow = true;
                        }
                    } else {
                        if (StringUtils.isNotBlank(endDateString)) {
                            searchCriteria.setStudyDateRange(new DqrDateRange(null, formatter.parse(endDateString)));
                            areThereSearchCriteriaForThisRow = true;
                        } else {
                            //Range is open ended on both ends so no search criteria should be added.
                        }
                    }
                }
            }
            if (dobColumn != -1 && StringUtils.isNotBlank(row.get(dobColumn))) {
                searchCriteria.setDob(removeExtraQuotes(row.get(dobColumn)));
                areThereSearchCriteriaForThisRow = true;
            }
            if (modalityColumn != -1 && StringUtils.isNotBlank(row.get(modalityColumn))) {
                searchCriteria.setModality(removeExtraQuotes(row.get(modalityColumn)));
                areThereSearchCriteriaForThisRow = true;
            }
            if (!areThereSearchCriteriaForThisRow && !allowRowThatGetsAllStudiesOnPacs) {
                throw new Exception("No search criteria found. Users must specify at least one valid search criteria.");
            }

            final PacsSearchResults<String, Study> studies = getStudiesByExample(user, pacs, searchCriteria);

            final Map<String, String> anonMapForThisRow = new HashMap<>();
            for (final Map.Entry<Integer, String> entry : columnToColumnHeaderMap.entrySet()) {
                final String stringToRemapTo = row.get(entry.getKey());
                if (StringUtils.isNotBlank(stringToRemapTo)) {
                    if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo) || StringUtils.equals(CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER, stringToRemapTo)) {
                        anonMapForThisRow.put(entry.getValue(), "\"\"");
                    } else {
                        anonMapForThisRow.put(entry.getValue(), stringToRemapTo);
                    }
                }
            }
            resultRows.add(new FindRow(searchCriteria, anonMapForThisRow, new ArrayList<>(studies.getResults())));
        }
        return resultRows;
    }

    @Override
    public boolean processSpreadsheetImport(Map<String, StudyImportInformation> studiesToImport, UserI user, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception {
        final Pacs pacs = _pacsEntityService.retrieve(pacsId);
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
                final String currAnonScript         = getAnonScript(studyInfo);
                List<String> seriesDescriptionsList = studyInfo.getSeriesDescriptions();
                List<String> seriesInstanceUIDs     = studyInfo.getSeriesInstanceUIDs();
                if (StringUtils.isNotBlank(currAnonScript) && !importEvenIfCustomProcessingIsOff) {
                    DicomSCPInstance scpInstance = _dicomSCPManager.getDicomSCPInstance(aeTitle, Integer.parseInt(port));
                    if (scpInstance == null) {
                        throw new Exception("Invalid DICOM SCP Receiver ID.");
                    }
                    if (!scpInstance.isEnabled()) {
                        throw new Exception("Invalid DICOM SCP Receiver ID.");
                    }
                    if (!scpInstance.isCustomProcessing()) {
                        throw new Exception("You are trying to remap DICOM fields. For this to work, custom processing must be enabled for this SCP receiver.");
                    }
                    List<ArchiveProcessorInstance> processorInstances = _archiveProcessorInstanceService.getAllEnabledSiteProcessorsForAe(ae);
                    if (processorInstances.isEmpty()) {
                        throw new Exception("You are trying to remap DICOM fields. For this to work, you must have a remapping processor for this SCP receiver.");
                    } else {
                        boolean hasProcessorOtherThanSiteAnon = false;
                        for (ArchiveProcessorInstance instance : processorInstances) {
                            if (!StringUtils.equals(instance.getProcessorClass(), "org.nrg.xnat.processors.MizerArchiveProcessor")) {
                                hasProcessorOtherThanSiteAnon = true;
                            }
                        }
                        if (!hasProcessorOtherThanSiteAnon) {
                            throw new Exception("You are trying to remap DICOM fields. For this to work, you must have a remapping processor for this SCP receiver.");
                        }
                    }
                }

                //TODO: We should just be able to uncomment the setStudyScript call and remove the 11 lines below it, but I'm having a build issue with the updated XNAT code not being picked up. This should be changed as soon as those issues are resolved.
                String       login = _userProvider.getLogin();
                final String path  = "/studies/" + currStudy;
                log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, currStudy);

                final PacsSearchResults<String, Series> series        = getSeriesByStudyUid(user, pacs, currStudy);
                Collection<Series>                      seriesResults = series.getResults();

                List<String> seriesToImport = new ArrayList<>();
                if (CollectionUtils.isEmpty(seriesInstanceUIDs)) {
                    if (CollectionUtils.isEmpty(seriesDescriptionsList)) {
                        //Import all the series in the study
                        for (Series currSeries : seriesResults) {
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
                        for (Series currSeries : seriesResults) {
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
                        for (Series currSeries : seriesResults) {
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
                        for (Series currSeries : seriesResults) {
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

                StringBuilder _seriesIdsString = new StringBuilder();

                for (String currSeries : seriesToImport) {
                    if (_seriesIdsString.length() != 0) {
                        _seriesIdsString.append(",");
                    }
                    _seriesIdsString.append(currSeries);
                }
                if (StringUtils.isNotBlank(_seriesIdsString.toString())) {
                    try {
                        QueuedPacsRequest pacsReq = new QueuedPacsRequest();
                        pacsReq.setPacsId(pacsId);
                        pacsReq.setUsername(user.getUsername());
                        pacsReq.setXnatProject(project);
                        pacsReq.setStudyInstanceUid(currStudy);
                        pacsReq.setSeriesIds(_seriesIdsString.toString());
                        pacsReq.setDestinationAeTitle(aeTitle);
                        pacsReq.setStudyDate(currStudyDate);
                        pacsReq.setStudyId(currStudyId);
                        pacsReq.setAccessionNumber(currAccessionNumber);
                        pacsReq.setPatientId(currPatientId);
                        pacsReq.setPatientName(currPatientName);
                        if (currAnonScript != null) {
                            pacsReq.setRemappingScript(currAnonScript);
                        }
                        if (multiStudy) {
                            pacsReq.setPriority(PacsRequest.STANDARD_PRIORITY);
                        } else {
                            pacsReq.setPriority(PacsRequest.HIGH_PRIORITY);
                        }
                        pacsReq.setStatus(PacsRequest.QUEUED_STATUS_TEXT);
                        pacsReq.setQueuedTime(new Date());

                        _queuedPacsRequestService.create(pacsReq);
                        valueToReturn = false;
                    } catch (Exception e) {
                        final Throwable cause = e.getCause();
                        if (cause instanceof CMoveFailureException) {
                            final CMoveFailureException failure = (CMoveFailureException) cause;
                            log.error("C-MOVE operation failed:\n" + failure.getMessage(), failure);
                        }
                    }
                }
            }
        }
        return valueToReturn;
    }

    @Override
    public boolean processSpreadsheetImportFromRows(UserI user, List<CsvRow> rows, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception {
        final Pacs pacs = _pacsEntityService.retrieve(pacsId);
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
        boolean            valueToReturn                 = true;
        Map<Study, String> studiesListMappedToAnonScript = new HashMap<>();
        for (CsvRow row : rows) {
            if (row != null && row.getStudies() != null) {
                for (Study currStudy : row.getStudies()) {
                    if (currStudy != null && !studiesListMappedToAnonScript.containsKey(currStudy)) {
                        String anon = row.getAnonScript();
                        studiesListMappedToAnonScript.put(currStudy, anon);
                        if (StringUtils.isNotBlank(anon) && !importEvenIfCustomProcessingIsOff) {
                            DicomSCPInstance scpInstance = _dicomSCPManager.getDicomSCPInstance(aeTitle, Integer.parseInt(port));
                            if (scpInstance == null) {
                                throw new Exception("Invalid DICOM SCP Receiver ID.");
                            }
                            if (!scpInstance.isEnabled()) {
                                throw new Exception("Invalid DICOM SCP Receiver ID.");
                            }
                            if (!scpInstance.isCustomProcessing()) {
                                throw new Exception("You are trying to remap DICOM fields. For this to work, custom processing must be enabled for this SCP receiver.");
                            }
                            List<ArchiveProcessorInstance> processorInstances = _archiveProcessorInstanceService.getAllEnabledSiteProcessorsForAe(ae);
                            if (processorInstances.isEmpty()) {
                                throw new Exception("You are trying to remap DICOM fields. For this to work, you must have a remapping processor for this SCP receiver.");
                            } else {
                                boolean hasProcessorOtherThanSiteAnon = false;
                                for (ArchiveProcessorInstance instance : processorInstances) {
                                    if (!StringUtils.equals(instance.getProcessorClass(), "org.nrg.xnat.processors.MizerArchiveProcessor")) {
                                        hasProcessorOtherThanSiteAnon = true;
                                    }
                                }
                                if (!hasProcessorOtherThanSiteAnon) {
                                    throw new Exception("You are trying to remap DICOM fields. For this to work, you must have a remapping processor for this SCP receiver.");
                                }
                            }
                        }
                    }
                }
            }
        }
        Set<Map.Entry<Study, String>> studiesSet = studiesListMappedToAnonScript.entrySet();
        boolean                       multiStudy = studiesSet.size() > 1;
        for (Map.Entry<Study, String> entry : studiesSet) {
            Study  currStudy      = entry.getKey();
            String currAnonScript = entry.getValue();

            //TODO: We should just be able to uncomment the setStudyScript call and remove the 11 lines below it, but I'm having a build issue with the updated XNAT code not being picked up. This should be changed as soon as those issues are resolved.
            //            DefaultAnonUtils.setStudyScript(AdminUtils.getAdminUser().getLogin(), currAnonScript, currStudy.getStudyInstanceUid());
            String       login   = _userProvider.getLogin();
            String       studyId = currStudy.getStudyInstanceUid();
            final String path    = "/studies/" + studyId;
            if (log.isDebugEnabled()) {
                log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, studyId);
            }
            if (studyId == null) {
                _configService.replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript);
            } else {
                _configService.replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, studyId);
                _configService.enable(login, "", DicomEdit.ToolName, path, Scope.Site, studyId);
            }


            final PacsSearchResults<String, Series> series           = getSeriesByStudy(user, pacs, currStudy);
            StringBuilder                           _seriesIdsString = new StringBuilder();
            ArrayList<String>                       seriesIdsList    = new ArrayList<>();
            Object[]                                seriesResults    = series.getResults().toArray();
            for (int index = 0; index < seriesResults.length; index++) {
                if (index > 0) {
                    _seriesIdsString.append(",");
                }
                String result = ((Series) seriesResults[index]).getSeriesInstanceUid();
                _seriesIdsString.append(result);
                seriesIdsList.add(result);
            }

            try {
                QueuedPacsRequest pacsReq = new QueuedPacsRequest();
                pacsReq.setPacsId(pacsId);
                pacsReq.setUsername(user.getUsername());
                pacsReq.setXnatProject(project);
                pacsReq.setStudyInstanceUid(currStudy.getStudyInstanceUid());
                pacsReq.setSeriesIds(_seriesIdsString.toString());
                pacsReq.setDestinationAeTitle(aeTitle);
                if (multiStudy) {
                    pacsReq.setPriority(PacsRequest.STANDARD_PRIORITY);
                } else {
                    pacsReq.setPriority(PacsRequest.HIGH_PRIORITY);
                }
                pacsReq.setStatus(PacsRequest.QUEUED_STATUS_TEXT);
                pacsReq.setQueuedTime(new Date());

                _queuedPacsRequestService.create(pacsReq);
                valueToReturn = false;
            } catch (Exception e) {
                final Throwable cause = e.getCause();
                if (cause instanceof CMoveFailureException) {
                    final CMoveFailureException failure = (CMoveFailureException) cause;
                    log.error("C-MOVE operation failed:\n" + failure.getMessage(), failure);
                }
            }
        }
        return valueToReturn;
    }

    @Override
    public void processSpreadsheetImport(UserI user, File csv, String ae, String project, long pacsId) throws PacsNotFoundException {
        final Pacs pacs = _pacsEntityService.retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException(pacsId);
        }
        //ArrayList<Study> studiesList = new ArrayList<>();
        Map<Study, String> studiesListMappedToAnonScript = new HashMap<>();
        try {
            List<List<String>> rows                  = FileUtils.CSVFileToArrayList(csv);
            List<String>       columnHeaders         = rows.get(0); //The first row must contain the column headers
            int                accessionNumberColumn = columnHeaders.indexOf("Accession Number");
            int                studyDateColumn       = columnHeaders.indexOf("Study Date");
            int                patientIdColumn       = columnHeaders.indexOf("Patient ID");
            int                lastNameColumn        = columnHeaders.indexOf("Last Name");
            int                firstNameColumn       = columnHeaders.indexOf("First Name");
            int                dobColumn             = columnHeaders.indexOf("DOB");
            int                modalityColumn        = columnHeaders.indexOf("Modality");

            HashMap<Integer, String> columnToDicomTagMap = new HashMap<>();
            for (Map.Entry<String, String> entry : HEADER_TO_TAG_MAP.entrySet()) {
                int indexOfHeader = columnHeaders.indexOf(entry.getKey());
                if (indexOfHeader != -1) {
                    columnToDicomTagMap.put(indexOfHeader, entry.getValue());
                }
            }

            for (int index = 1; index < rows.size(); index++) {//Skip the first row since that is a header row
                List<String>             row            = rows.get(index);
                final PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
                if (accessionNumberColumn != -1 && StringUtils.isNotBlank(row.get(accessionNumberColumn))) {
                    searchCriteria.setAccessionNumber(row.get(accessionNumberColumn));
                }
                if ((lastNameColumn != -1 && StringUtils.isNotBlank(row.get(lastNameColumn))) || (firstNameColumn != -1 && StringUtils.isNotBlank(row.get(firstNameColumn)))) {
                    String lastName  = (lastNameColumn == -1 || StringUtils.isBlank(row.get(lastNameColumn))) ? "" : row.get(lastNameColumn);
                    String firstName = (firstNameColumn == -1 || StringUtils.isBlank(row.get(firstNameColumn))) ? "" : row.get(firstNameColumn);
                    if (StringUtils.isNotBlank(firstName)) {
                        searchCriteria.setPatientName(lastName + "," + firstName);
                    } else {
                        searchCriteria.setPatientName(lastName);
                    }
                }
                if (patientIdColumn != -1 && StringUtils.isNotBlank(row.get(patientIdColumn))) {
                    searchCriteria.setPatientId(row.get(patientIdColumn));
                }
                if (studyDateColumn != -1 && StringUtils.isNotBlank(row.get(studyDateColumn))) {
                    String           studyDateCell = row.get(studyDateColumn);
                    SimpleDateFormat formatter     = new SimpleDateFormat("yyyyMMdd");
                    int              dashIndex     = studyDateCell.indexOf("-");
                    if (dashIndex == -1) {
                        Date     dateObject = formatter.parse(studyDateCell);
                        Calendar c          = Calendar.getInstance();
                        c.setTime(dateObject);
                        c.add(Calendar.DATE, 1);
                        Date endOfDay = c.getTime();

                        searchCriteria.setStudyDateRange(new DqrDateRange(dateObject, endOfDay));
                    } else {
                        String startDateString = studyDateCell.substring(0, dashIndex);
                        String endDateString   = studyDateCell.substring(dashIndex + 1);
                        if (StringUtils.isNotBlank(startDateString)) {
                            if (StringUtils.isNotBlank(endDateString)) {
                                searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), formatter.parse(endDateString)));
                            } else {
                                searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), null));
                            }
                        } else {
                            if (StringUtils.isNotBlank(endDateString)) {
                                searchCriteria.setStudyDateRange(new DqrDateRange(null, formatter.parse(endDateString)));
                            } else {
                                //Range is open ended on both ends so no search criteria should be added.
                            }
                        }
                    }
                }
                if (dobColumn != -1 && StringUtils.isNotBlank(row.get(dobColumn))) {
                    searchCriteria.setDob(row.get(dobColumn));
                }
                if (modalityColumn != -1 && StringUtils.isNotBlank(row.get(modalityColumn))) {
                    searchCriteria.setModality(row.get(modalityColumn));
                }


                final PacsSearchResults<String, Study> studies = getStudiesByExample(user, pacs, searchCriteria);

                boolean       anonymizeThisRow     = false;
                StringBuilder anonScriptForThisRow = new StringBuilder("version \"6.1\"" + System.lineSeparator());
                for (Map.Entry<Integer, String> entry : columnToDicomTagMap.entrySet()) {
                    String stringToRemapTo = row.get(entry.getKey());
                    if (StringUtils.isNotBlank(stringToRemapTo)) {
//                        if (StringUtils.equals(DELETE_SIGNIFIER,stringToRemapTo)) {
//                            anonScriptForThisRow += "- " + entry.getValue() + System.lineSeparator();
//                            anonymizeThisRow = true;
//                        } else if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo)) {
                        if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo) || StringUtils.equals(CLEAR_SIGNIFIER + CLEAR_SIGNIFIER + CLEAR_SIGNIFIER, stringToRemapTo)) {
                            anonScriptForThisRow.append(entry.getValue()).append(" := \"\"").append(System.lineSeparator());
                            anonymizeThisRow = true;
                        } else {
                            anonScriptForThisRow.append(entry.getValue()).append(" := \"").append(stringToRemapTo).append("\"").append(System.lineSeparator());
                            anonymizeThisRow = true;
                        }
                    }
                }

                for (Study currStudy : studies.getResults()) {
                    if (currStudy != null && !studiesListMappedToAnonScript.containsKey(currStudy)) {
                        if (anonymizeThisRow) {
                            studiesListMappedToAnonScript.put(currStudy, anonScriptForThisRow.toString());
                        } else {
                            studiesListMappedToAnonScript.put(currStudy, null);
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            log.error("Failed to get studies list from spreadsheet.", e);
        }
        Set<Map.Entry<Study, String>> studiesSet = studiesListMappedToAnonScript.entrySet();
        boolean                       multiStudy = studiesSet.size() > 1;
        for (Map.Entry<Study, String> entry : studiesSet) {
            Study  currStudy      = entry.getKey();
            String currAnonScript = entry.getValue();


            //TODO: We should just be able to uncomment the setStudyScript call and remove the 11 lines below it, but I'm having a build issue with the updated XNAT code not being picked up. This should be changed as soon as those issues are resolved.
//            DefaultAnonUtils.setStudyScript(AdminUtils.getAdminUser().getLogin(), currAnonScript, currStudy.getStudyInstanceUid());
//            String login   = AdminUtils.getAdminUser().getLogin();
//            String studyId = currStudy.getStudyInstanceUid();
//            final String path = "/studies/" + studyId;
//            log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, studyId);
//            if (studyId == null) {
//                _configService.replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript);
//            } else {
//                _configService.replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, studyId);
//                _configService.enable(login, "", DicomEdit.ToolName, path, Scope.Site, studyId);
//            }


            final PacsSearchResults<String, Series> series           = getSeriesByStudy(user, pacs, currStudy);
            StringBuilder                           _seriesIdsString = new StringBuilder();
            ArrayList<String>                       seriesIdsList    = new ArrayList<>();
            Object[]                                seriesResults    = series.getResults().toArray();
            for (int index = 0; index < seriesResults.length; index++) {
                if (index > 0) {
                    _seriesIdsString.append(",");
                }
                String result = ((Series) seriesResults[index]).getSeriesInstanceUid();
                _seriesIdsString.append(result);
                seriesIdsList.add(result);
            }

            try {
                QueuedPacsRequest pacsReq = new QueuedPacsRequest();
                pacsReq.setPacsId(pacsId);
                pacsReq.setUsername(user.getUsername());
                pacsReq.setXnatProject(project);
                pacsReq.setStudyInstanceUid(currStudy.getStudyInstanceUid());
                pacsReq.setSeriesIds(_seriesIdsString.toString());
                pacsReq.setDestinationAeTitle(ae);
                pacsReq.setRemappingScript(currAnonScript);
                if (multiStudy) {
                    pacsReq.setPriority(PacsRequest.STANDARD_PRIORITY);
                } else {
                    pacsReq.setPriority(PacsRequest.HIGH_PRIORITY);
                }
                pacsReq.setStatus(PacsRequest.QUEUED_STATUS_TEXT);
                pacsReq.setQueuedTime(new Date());
                _queuedPacsRequestService.create(pacsReq);
//                }
//            } catch (final PacsNotFoundException exception) {
//                _log.warn("PACS not found somehow", exception);
//            } catch (final PacsNotQueryableException exception) {
//                _log.warn("PACS not queryable somehow", exception);
//            } catch (final PacsNotStorableException exception) {
//                _log.warn("PACS not storable somehow", exception);
//            } catch (final PacsNotAvailableException exception) {
//                _log.warn("PACS not available at this time", exception);
//            } catch (PersistentWorkflowUtils.ActionNameAbsent e) {
//                _log.warn("Error creating new workflow event", e);
//            } catch (PersistentWorkflowUtils.IDAbsent e) {
//                _log.warn("ID absent when creating new workflow event", e);
//            } catch (PersistentWorkflowUtils.JustificationAbsent e) {
//                _log.warn("Justification absent but required when creating new workflow event", e);
            } catch (Exception e) {
                final Throwable cause = e.getCause();
                if (cause instanceof CMoveFailureException) {
                    final CMoveFailureException failure = (CMoveFailureException) cause;
                    log.error("C-MOVE operation failed:\n" + failure.getMessage(), failure);
                }
            }
        }
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

    private static String getAnonScript(final StudyImportInformation studyInfo) {
        final String currAnonScript = studyInfo.getAnonScript();
        if (StringUtils.isNotBlank(currAnonScript)) {
            return currAnonScript;
        }
        final Map<String, String> relabelMap = studyInfo.getRelabelMap();
        return relabelMap != null && !relabelMap.isEmpty() ? generateAnonScriptFromMap(relabelMap) : null;
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

    private static String generateAnonScriptFromMap(final Map<String, String> relabelMap) {
        if (relabelMap == null || relabelMap.isEmpty()) {
            return null;
        }
        final StringBuilder currAnonScript = new StringBuilder("version \"6.1\"" + System.lineSeparator());
        for (final Map.Entry<String, String> entry : relabelMap.entrySet()) {
            final String[] tags     = StringUtils.split(HEADER_TO_TAG_MAP.get(entry.getKey()), ":");
            final String   newValue = entry.getValue();
            if (StringUtils.isNotBlank(newValue) && tags != null) {
                for (String tag : tags) {
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

    private static final String              CLEAR_SIGNIFIER   = "\"\"";
    private static final Map<String, String> HEADER_TO_TAG_MAP = createHeaderToTagMap();

    private final DqrPreferences _preferences;
}
