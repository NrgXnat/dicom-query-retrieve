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
import org.apache.commons.lang.StringUtils;
import org.nrg.config.exceptions.ConfigServiceException;
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
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.restlet.NullValueSerializer;
import org.nrg.dqr.util.DqrRuntimeException;
import org.nrg.dqr.util.SimpleCsvRow;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xft.utils.FileUtils;
import org.nrg.xnat.entities.ArchiveProcessorInstance;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.processor.services.ArchiveProcessorInstanceService;
import org.nrg.xnat.restlet.extensions.*;
import org.nrg.xnat.utils.*;
import org.nrg.xnat.utils.DqrDateRange;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import java.io.File;
import java.text.SimpleDateFormat;
import java.util.*;
import org.nrg.dqr.util.CsvRow;

@Service
public class BasicPacsService implements PacsService {

    private static final Logger _log = LoggerFactory.getLogger(BasicPacsService.class);
    //private static final String DELETE_SIGNIFIER = "DELETE";
    private static final String CLEAR_SIGNIFIER = "\"\"";

    private static final Map<String, String> HEADER_TO_TAG_MAP = createHeaderToTagMap();

    private final DqrPreferences _preferences;

    public BasicPacsService(final DqrPreferences preferences) {
        _preferences = preferences;
    }

    private static Map<String, String> createHeaderToTagMap() {
        return new HashMap<String, String>() {{
            put("Accession Number Remapping", "(0008,0050)");
            put("Study Date Remapping", "(0008,0020)");
            put("Study ID Remapping", "(0020,0010)");
            put("Patient ID Remapping", "(0010,0020)");
            put("Patient Name Remapping", "(0010,0010)");
            put("Patient Birth Date Remapping", "(0010,0030)");
        }};
    }

    @Override
    public boolean canConnect(UserI user, final Pacs pacs){
        try{
            if(buildCEchoSCU(pacs).canConnect()){
                return true;
            }
        }
        catch(Throwable e) {
        }
        return false;
    }

    @Override
    public PacsSearchResults<String, Patient> getPatientsByExample(final UserI user, final Pacs pacs,
                                                                   final PacsSearchCriteria searchCriteria) {
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
        PacsEntityService pacsEntityService = getPacsEntityService();
        Pacs pacs = pacsEntityService.retrieve(request.getPacsId());
        String destinationAeAndPort = request.getDestinationAeTitle();
        String destinationAe = destinationAeAndPort;
        if (destinationAe != null && destinationAe.contains(":")) {
            String[] parts = destinationAe.split(":");
            destinationAe = parts[0];
        }
        if(!pacs.isQueryable()) {
            throw new PacsNotQueryableException();
        }
        else if(!aeIsStorable(destinationAeAndPort)){
            throw new PacsNotStorableException();
        }
        else {
            try {

                final Study study = assignStudyToProject(request.getXnatProject(), request.getStudyInstanceUid(), request.getUsername());

                for (String seriesId : Arrays.asList(request.getSeriesIds().split(","))) {
                    seriesId = seriesId.trim();
                    if (_log.isDebugEnabled()) {
                        _log.debug("Requesting series " + seriesId + " for study instance UID " + request.getStudyInstanceUid());
                    }
                    Series series = new Series(seriesId);

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
                _log.warn("C-MOVE target not found somehow: PACS [ aeTitle: " + pacs.getAeTitle() + ", ", exception);
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

    private CEchoSCU buildCEchoSCU(final Pacs pacs) throws PacsNotQueryableException {
        return new Dcm4cheToolCEchoSCU(_preferences, buildDicomConnectionProperties(pacs));
    }

    private CFindSCU buildCFindSCU(final Pacs pacs) throws PacsNotQueryableException {
        if(!pacs.isQueryable()){
            throw new PacsNotQueryableException();
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
        DicomSCPInstance firstXnatScp = XDAT.getContextService().getBean(DicomSCPManager.class).getDicomSCPInstances()
                .values().iterator().next();
        final String localAETitle = firstXnatScp.getAeTitle();
        return new DicomConnectionProperties(localAETitle, pacs);
    }

    private DicomConnectionProperties buildDicomConnectionProperties(final Pacs pacs, final String receiverAETitle) {
        if(!StringUtils.isBlank(receiverAETitle)) {
            return new DicomConnectionProperties(receiverAETitle, pacs);
        }
        else{
            return buildDicomConnectionProperties(pacs);
        }
    }

    private OrmStrategy getOrmStrategy(final Pacs pacs) {
        try {
            return XDAT.getContextService().getBean(pacs.getOrmStrategySpringBeanId(), OrmStrategy.class);
        } catch (final Exception e) {
            throw new DqrRuntimeException(String.format("Failed to load the ORM strategy defined by bean '%s'",
                    pacs.getOrmStrategySpringBeanId()), e);
        }
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
        try {
            return Boolean.valueOf(StringUtils.trimToEmpty(XDAT.getSiteConfigurationProperty("leavePacsAuditTrail")));
        } catch (final ConfigServiceException e) {
            throw new RuntimeException(e);
        }
    }

    private static final ObjectMapper MAPPER = new ObjectMapper();
    static {
        final DefaultSerializerProvider provider = new DefaultSerializerProvider.Impl();
        provider.setNullValueSerializer(new NullValueSerializer());
        MAPPER.setSerializerProvider(provider);
    }

    @Override
    public boolean aeIsStorable(final String ae){
        //The user is able to store to an AE if there is either an XNAT SCP receiver with that AE or there is an enabled PACS with that AE for which storable=true
        Collection<DicomSCPInstance> scps = XDAT.getContextService().getBean(DicomSCPManager.class).getDicomSCPInstances().values();
        boolean hasPort = ae.contains(":");
        for (DicomSCPInstance scp : scps){
            if(((hasPort&&StringUtils.equals(scp.getAeTitle()+":"+scp.getPort(),ae))||(!hasPort&&StringUtils.equals(scp.getAeTitle(),ae))) && scp.isEnabled()){
                return true;
            }
        }
//
//        final List<Pacs> allPacs = getPacsEntityService().findAllStorable();
//        for (Pacs pacsToCheck : allPacs){
//            if(StringUtils.equals(pacsToCheck.getAeTitle(),ae)){
//                return true;
//            }
//        }
        return false;
    }

    @Override
    public List<CsvRow> extractImportRequestFromCsv(UserI user, File csv, long pacsId, boolean allowRowThatGetsAllStudiesOnPacs) throws Exception {
        Pacs pacs = getPacsEntityService().retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException();
        }
        ArrayList<CsvRow> resultRows = new ArrayList<>();

        List<List<String>> rows = FileUtils.CSVFileToArrayList(csv);
        List<String> columnHeaders = rows.get(0); //The first row must contain the column headers
        int accessionNumberColumn = columnHeaders.indexOf("Accession Number");
        int studyDateColumn = columnHeaders.indexOf("Study Date");
        int patientIdColumn = columnHeaders.indexOf("Patient ID");
        int lastNameColumn = columnHeaders.indexOf("Last Name");
        int firstNameColumn = columnHeaders.indexOf("First Name");
        int dobColumn = columnHeaders.indexOf("DOB");
        int modalityColumn = columnHeaders.indexOf("Modality");

        HashMap<Integer, String> columnToDicomTagMap = new HashMap<>();
        for (Map.Entry<String, String> entry : HEADER_TO_TAG_MAP.entrySet()) {
            int indexOfHeader = columnHeaders.indexOf(entry.getKey());
            if (indexOfHeader != -1) {
                columnToDicomTagMap.put(indexOfHeader, entry.getValue());
            }
        }

        for (int index = 1; index < rows.size(); index++) {//Skip the first row since that is a header row
            List<String> row = rows.get(index);
            boolean areThereSearchCriteriaForThisRow = false;

            final PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
            if (accessionNumberColumn != -1 && StringUtils.isNotBlank(row.get(accessionNumberColumn))) {
                searchCriteria.setAccessionNumber(row.get(accessionNumberColumn));
                areThereSearchCriteriaForThisRow = true;
            }
            if ((lastNameColumn != -1 && StringUtils.isNotBlank(row.get(lastNameColumn))) || (firstNameColumn != -1 && StringUtils.isNotBlank(row.get(firstNameColumn)))) {
                String lastName = (lastNameColumn==-1 || StringUtils.isBlank(row.get(lastNameColumn))) ? "" : row.get(lastNameColumn);
                String firstName = (firstNameColumn==-1 || StringUtils.isBlank(row.get(firstNameColumn))) ? "" : row.get(firstNameColumn);
                searchCriteria.setPatientName(lastName+","+firstName);
                areThereSearchCriteriaForThisRow = true;
            }
            if (patientIdColumn != -1 && StringUtils.isNotBlank(row.get(patientIdColumn))) {
                searchCriteria.setPatientId(row.get(patientIdColumn));
                areThereSearchCriteriaForThisRow = true;
            }
            if (studyDateColumn != -1 && StringUtils.isNotBlank(row.get(studyDateColumn))) {
                String studyDateCell = row.get(studyDateColumn);
                SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
                int dashIndex = studyDateCell.indexOf("-");
                if(dashIndex==-1){
                    Date dateObject = formatter.parse(studyDateCell);
                    Calendar c = Calendar.getInstance();
                    c.setTime(dateObject);
                    c.add(Calendar.DATE, 1);
                    Date endOfDay = c.getTime();

                    searchCriteria.setStudyDateRange(new DqrDateRange(dateObject, endOfDay));
                    areThereSearchCriteriaForThisRow = true;
                }
                else{
                    String startDateString = studyDateCell.substring(0,dashIndex);
                    String endDateString = studyDateCell.substring(dashIndex+1,studyDateCell.length());
                    if(StringUtils.isNotBlank(startDateString)){
                        if(StringUtils.isNotBlank(endDateString)){
                            searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), formatter.parse(endDateString)));
                            areThereSearchCriteriaForThisRow = true;
                        }
                        else{
                            searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), null));
                            areThereSearchCriteriaForThisRow = true;
                        }
                    }
                    else{
                        if(StringUtils.isNotBlank(endDateString)){
                            searchCriteria.setStudyDateRange(new DqrDateRange(null, formatter.parse(endDateString)));
                            areThereSearchCriteriaForThisRow = true;
                        }
                        else{
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
            if(!areThereSearchCriteriaForThisRow && !allowRowThatGetsAllStudiesOnPacs){
                throw new Exception("No search criteria found. Users must specify at least one valid search criteria.");
            }

            final PacsSearchResults<String, Study> studies = getStudiesByExample(
                    XDAT.getUserDetails(), pacs, searchCriteria);

            boolean anonymizeThisRow = false;
            String anonScriptForThisRow = "version \"6.1\""+System.lineSeparator();
            for(Map.Entry<Integer, String> entry : columnToDicomTagMap.entrySet()){
                String stringToRemapTo = row.get(entry.getKey());
                if(StringUtils.isNotBlank(stringToRemapTo)) {
//                        if (StringUtils.equals(DELETE_SIGNIFIER,stringToRemapTo)) {
//                            anonScriptForThisRow += "- " + entry.getValue() + System.lineSeparator();
//                            anonymizeThisRow = true;
//                        } else if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo)) {
                    if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo)) {
                        anonScriptForThisRow += entry.getValue() + " := \"\"" + System.lineSeparator();
                        anonymizeThisRow = true;
                    } else {
                        anonScriptForThisRow += entry.getValue() + " := \"" + stringToRemapTo + "\"" + System.lineSeparator();
                        anonymizeThisRow = true;
                    }
                }
            }
            if(!anonymizeThisRow){
                anonScriptForThisRow = null;
            }
            CsvRow currResult = new CsvRow(searchCriteria,anonScriptForThisRow,new ArrayList<>(studies.getResults()));
            resultRows.add(currResult);
        }
        return resultRows;
    }

    @Override
    public boolean processSpreadsheetImportFromRows(UserI user, List<CsvRow> rows, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception {
        Pacs pacs = getPacsEntityService().retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException();
        }
        String aeTitle = ae;
        String port = "";
        if(ae!=null && ae.contains(":")){
            String[] parts = ae.split(":");
            aeTitle = parts[0];
            port = parts[1];
        }
        boolean valueToReturn = true;
        Map<Study,String> studiesListMappedToAnonScript = new HashMap<>();
        for(CsvRow row : rows) {
            if(row!=null&&row.getStudies()!=null) {
                for (Study currStudy : row.getStudies()) {
                    if (currStudy != null && !studiesListMappedToAnonScript.containsKey(currStudy)) {
                        String anon = row.getAnonScript();
                        studiesListMappedToAnonScript.put(currStudy, anon);
                        if (StringUtils.isNotBlank(anon) && !importEvenIfCustomProcessingIsOff) {
                            DicomSCPInstance scpInstance = getScpManager().getDicomSCPInstance(aeTitle, Integer.parseInt(port));
                            if (scpInstance == null) {
                                throw new Exception("Invalid DICOM SCP Receiver ID.");
                            }
                            if (!scpInstance.isEnabled()) {
                                throw new Exception("Invalid DICOM SCP Receiver ID.");
                            }
                            if (!scpInstance.getCustomProcessing()) {
                                throw new Exception("You are trying to remap DICOM fields. For this to work, custom processing must be enabled for this SCP receiver.");
                            }
                            List<ArchiveProcessorInstance> processorInstances = getProcessorService().getAllEnabledSiteProcessorsForAe(ae);
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

        for(Map.Entry<Study, String> entry : studiesListMappedToAnonScript.entrySet()){
            Study currStudy = entry.getKey();
            String currAnonScript = entry.getValue();

           //TODO: We should just be able to uncomment the setStudyScript call and remove the 11 lines below it, but I'm having a build issue with the updated XNAT code not being picked up. This should be changed as soon as those issues are resolved.
//            DefaultAnonUtils.setStudyScript(AdminUtils.getAdminUser().getLogin(), currAnonScript, currStudy.getStudyInstanceUid());
            String login = AdminUtils.getAdminUser().getLogin();
            String studyId = currStudy.getStudyInstanceUid();
            final String path = "/studies/" + studyId;
            if (_log.isDebugEnabled()) {
                _log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, studyId);
            }
            if (studyId == null) {
                XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript);
            } else {
                XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, studyId);
                XDAT.getConfigService().enable(login, "", DicomEdit.ToolName, path, Scope.Site, studyId);
            }



            final PacsSearchResults<String, Series> series = getSeriesByStudy(XDAT.getUserDetails(), pacs, currStudy);
            String _seriesIdsString = "";
            ArrayList<String> seriesIdsList = new ArrayList<>();
            Object[] seriesResults = series.getResults().toArray();
            for(int index = 0; index<seriesResults.length; index++){
                if (index > 0) {
                    _seriesIdsString += ",";
                }
                String result = ((Series)seriesResults[index]).getSeriesInstanceUid();
                _seriesIdsString += result;
                seriesIdsList.add(result);
            }

            try {
                    QueuedPacsRequest pacsReq = new QueuedPacsRequest();
                    pacsReq.setPacsId(pacsId);
                    pacsReq.setUsername(user.getUsername());
                    pacsReq.setXnatProject(project);
                    pacsReq.setStudyInstanceUid(currStudy.getStudyInstanceUid());
                    pacsReq.setSeriesIds(_seriesIdsString);
                    pacsReq.setDestinationAeTitle(aeTitle);
                    pacsReq.setQueuedTime(new Date());

                    XDAT.getContextService().getBean(QueuedPacsRequestService.class).create(pacsReq);
                    valueToReturn = false;
            } catch (Exception e) {
                final Throwable cause = e.getCause();
                if (cause == null || !(cause instanceof Exception)) {
                } else if (cause instanceof CMoveFailureException) {
                    final CMoveFailureException failure = (CMoveFailureException) cause;
                    _log.error("C-MOVE operation failed:\n" + failure.getMessage(), failure);
                }
            }
        }
        return valueToReturn;
    }

    @Override
    public boolean processSpreadsheetImportFromSimpleRows(UserI user, List<SimpleCsvRow> rows, String ae, String project, long pacsId, boolean importEvenIfCustomProcessingIsOff) throws Exception {
        Pacs pacs = getPacsEntityService().retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException();
        }
        String aeTitle = ae;
        String port = "";
        if(ae!=null && ae.contains(":")){
            String[] parts = ae.split(":");
            aeTitle = parts[0];
            port = parts[1];
        }
        boolean valueToReturn = true;
        Map<String,String> studiesListMappedToAnonScript = new HashMap<>();
        for(SimpleCsvRow row : rows) {
            if(row!=null&&row.getStudyInstanceUIDs()!=null) {
                for (String currStudy : row.getStudyInstanceUIDs()) {
                    if (currStudy != null && !studiesListMappedToAnonScript.containsKey(currStudy)) {
                        String anon = row.getAnonScript();
                        studiesListMappedToAnonScript.put(currStudy, anon);
                        if (StringUtils.isNotBlank(anon) && !importEvenIfCustomProcessingIsOff) {
                            DicomSCPInstance scpInstance = getScpManager().getDicomSCPInstance(aeTitle, Integer.parseInt(port));
                            if (scpInstance == null) {
                                throw new Exception("Invalid DICOM SCP Receiver ID.");
                            }
                            if (!scpInstance.isEnabled()) {
                                throw new Exception("Invalid DICOM SCP Receiver ID.");
                            }
                            if (!scpInstance.getCustomProcessing()) {
                                throw new Exception("You are trying to remap DICOM fields. For this to work, custom processing must be enabled for this SCP receiver.");
                            }
                            List<ArchiveProcessorInstance> processorInstances = getProcessorService().getAllEnabledSiteProcessorsForAe(ae);
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

        for(Map.Entry<String, String> entry : studiesListMappedToAnonScript.entrySet()){
            String currStudy = entry.getKey();
            String currAnonScript = entry.getValue();

            //TODO: We should just be able to uncomment the setStudyScript call and remove the 11 lines below it, but I'm having a build issue with the updated XNAT code not being picked up. This should be changed as soon as those issues are resolved.
//            DefaultAnonUtils.setStudyScript(AdminUtils.getAdminUser().getLogin(), currAnonScript, currStudy.getStudyInstanceUid());
            String login = AdminUtils.getAdminUser().getLogin();
            final String path = "/studies/" + currStudy;
            if (_log.isDebugEnabled()) {
                _log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, currStudy);
            }
            if (currStudy == null) {
                XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript);
            } else {
                XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, currStudy);
                XDAT.getConfigService().enable(login, "", DicomEdit.ToolName, path, Scope.Site, currStudy);
            }



            final PacsSearchResults<String, Series> series = getSeriesByStudyUid(XDAT.getUserDetails(), pacs, currStudy);
            String _seriesIdsString = "";
            ArrayList<String> seriesIdsList = new ArrayList<>();
            Object[] seriesResults = series.getResults().toArray();
            for(int index = 0; index<seriesResults.length; index++){
                if (index > 0) {
                    _seriesIdsString += ",";
                }
                String result = ((Series)seriesResults[index]).getSeriesInstanceUid();
                _seriesIdsString += result;
                seriesIdsList.add(result);
            }

            try {
                QueuedPacsRequest pacsReq = new QueuedPacsRequest();
                pacsReq.setPacsId(pacsId);
                pacsReq.setUsername(user.getUsername());
                pacsReq.setXnatProject(project);
                pacsReq.setStudyInstanceUid(currStudy);
                pacsReq.setSeriesIds(_seriesIdsString);
                pacsReq.setDestinationAeTitle(aeTitle);
                pacsReq.setQueuedTime(new Date());

                XDAT.getContextService().getBean(QueuedPacsRequestService.class).create(pacsReq);
                valueToReturn = false;
            } catch (Exception e) {
                final Throwable cause = e.getCause();
                if (cause == null || !(cause instanceof Exception)) {
                } else if (cause instanceof CMoveFailureException) {
                    final CMoveFailureException failure = (CMoveFailureException) cause;
                    _log.error("C-MOVE operation failed:\n" + failure.getMessage(), failure);
                }
            }
        }
        return valueToReturn;
    }

    @Override
    public void processSpreadsheetImport(UserI user, File csv, String ae, String project, long pacsId) throws PacsNotFoundException, ConfigServiceException {
        Pacs pacs = getPacsEntityService().retrieve(pacsId);
        if (pacs == null) {
            throw new PacsNotFoundException();
        }
        //ArrayList<Study> studiesList = new ArrayList<>();
        Map<Study,String> studiesListMappedToAnonScript = new HashMap<>();
        try {
            List<List<String>> rows = FileUtils.CSVFileToArrayList(csv);
            List<String> columnHeaders = rows.get(0); //The first row must contain the column headers
            int accessionNumberColumn = columnHeaders.indexOf("Accession Number");
            int studyDateColumn = columnHeaders.indexOf("Study Date");
            int patientIdColumn = columnHeaders.indexOf("Patient ID");
            int lastNameColumn = columnHeaders.indexOf("Last Name");
            int firstNameColumn = columnHeaders.indexOf("First Name");
            int dobColumn = columnHeaders.indexOf("DOB");
            int modalityColumn = columnHeaders.indexOf("Modality");

            HashMap<Integer, String> columnToDicomTagMap = new HashMap<>();
            for (Map.Entry<String, String> entry : HEADER_TO_TAG_MAP.entrySet()) {
                int indexOfHeader = columnHeaders.indexOf(entry.getKey());
                if (indexOfHeader != -1) {
                    columnToDicomTagMap.put(indexOfHeader, entry.getValue());
                }
            }

            for (int index = 1; index < rows.size(); index++) {//Skip the first row since that is a header row
                List<String> row = rows.get(index);
                final PacsSearchCriteria searchCriteria = new PacsSearchCriteria();
                if (accessionNumberColumn != -1 && StringUtils.isNotBlank(row.get(accessionNumberColumn))) {
                    searchCriteria.setAccessionNumber(row.get(accessionNumberColumn));
                }
                if ((lastNameColumn != -1 && StringUtils.isNotBlank(row.get(lastNameColumn))) || (firstNameColumn != -1 && StringUtils.isNotBlank(row.get(firstNameColumn)))) {
                    String lastName = (lastNameColumn==-1 || StringUtils.isBlank(row.get(lastNameColumn))) ? "" : row.get(lastNameColumn);
                    String firstName = (firstNameColumn==-1 || StringUtils.isBlank(row.get(firstNameColumn))) ? "" : row.get(firstNameColumn);
                    searchCriteria.setPatientName(lastName+","+firstName);
                }
                if (patientIdColumn != -1 && StringUtils.isNotBlank(row.get(patientIdColumn))) {
                    searchCriteria.setPatientId(row.get(patientIdColumn));
                }
                if (studyDateColumn != -1 && StringUtils.isNotBlank(row.get(studyDateColumn))) {
                    String studyDateCell = row.get(studyDateColumn);
                    SimpleDateFormat formatter = new SimpleDateFormat("yyyyMMdd");
                    int dashIndex = studyDateCell.indexOf("-");
                    if(dashIndex==-1){
                        Date dateObject = formatter.parse(studyDateCell);
                        Calendar c = Calendar.getInstance();
                        c.setTime(dateObject);
                        c.add(Calendar.DATE, 1);
                        Date endOfDay = c.getTime();

                        searchCriteria.setStudyDateRange(new DqrDateRange(dateObject, endOfDay));
                    }
                    else{
                        String startDateString = studyDateCell.substring(0,dashIndex);
                        String endDateString = studyDateCell.substring(dashIndex+1,studyDateCell.length());
                        if(StringUtils.isNotBlank(startDateString)){
                            if(StringUtils.isNotBlank(endDateString)){
                                searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), formatter.parse(endDateString)));
                            }
                            else{
                                searchCriteria.setStudyDateRange(new DqrDateRange(formatter.parse(startDateString), null));
                            }
                        }
                        else{
                            if(StringUtils.isNotBlank(endDateString)){
                                searchCriteria.setStudyDateRange(new DqrDateRange(null, formatter.parse(endDateString)));
                            }
                            else{
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


                final PacsSearchResults<String, Study> studies = getStudiesByExample(
                        XDAT.getUserDetails(), pacs, searchCriteria);

                boolean anonymizeThisRow = false;
                String anonScriptForThisRow = "version \"6.1\""+System.lineSeparator();
                for(Map.Entry<Integer, String> entry : columnToDicomTagMap.entrySet()){
                    String stringToRemapTo = row.get(entry.getKey());
                    if(StringUtils.isNotBlank(stringToRemapTo)) {
//                        if (StringUtils.equals(DELETE_SIGNIFIER,stringToRemapTo)) {
//                            anonScriptForThisRow += "- " + entry.getValue() + System.lineSeparator();
//                            anonymizeThisRow = true;
//                        } else if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo)) {
                        if (StringUtils.equals(CLEAR_SIGNIFIER, stringToRemapTo)) {
                            anonScriptForThisRow += entry.getValue() + " := \"\"" + System.lineSeparator();
                            anonymizeThisRow = true;
                        } else {
                            anonScriptForThisRow += entry.getValue() + " := \"" + stringToRemapTo + "\"" + System.lineSeparator();
                            anonymizeThisRow = true;
                        }
                    }
                }

                for (Study currStudy : studies.getResults()) {
                    if (currStudy != null && !studiesListMappedToAnonScript.containsKey(currStudy)) {
                        if(anonymizeThisRow){
                            studiesListMappedToAnonScript.put(currStudy, anonScriptForThisRow);
                        }
                        else{
                            studiesListMappedToAnonScript.put(currStudy, null);
                        }
                    }
                }
            }
        } catch (final Throwable e) {
            _log.error("Failed to get studies list from spreadsheet.", e);
        }
        for(Map.Entry<Study, String> entry : studiesListMappedToAnonScript.entrySet()){
            Study currStudy = entry.getKey();
            String currAnonScript = entry.getValue();



            //TODO: We should just be able to uncomment the setStudyScript call and remove the 11 lines below it, but I'm having a build issue with the updated XNAT code not being picked up. This should be changed as soon as those issues are resolved.
//            DefaultAnonUtils.setStudyScript(AdminUtils.getAdminUser().getLogin(), currAnonScript, currStudy.getStudyInstanceUid());
            String login = AdminUtils.getAdminUser().getLogin();
            String studyId = currStudy.getStudyInstanceUid();
//            final String path = "/studies/" + studyId;
//            if (_log.isDebugEnabled()) {
//                _log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, studyId);
//            }
//            if (studyId == null) {
//                XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript);
//            } else {
//                XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, studyId);
//                XDAT.getConfigService().enable(login, "", DicomEdit.ToolName, path, Scope.Site, studyId);
//            }



            final PacsSearchResults<String, Series> series = getSeriesByStudy(XDAT.getUserDetails(), pacs, currStudy);
            String _seriesIdsString = "";
            ArrayList<String> seriesIdsList = new ArrayList<>();
            Object[] seriesResults = series.getResults().toArray();
            for(int index = 0; index<seriesResults.length; index++){
                if (index > 0) {
                    _seriesIdsString += ",";
                }
                String result = ((Series)seriesResults[index]).getSeriesInstanceUid();
                _seriesIdsString += result;
                seriesIdsList.add(result);
            }

            try {
//                PacsEntityService pacsEntityService = getPacsEntityService();
//                boolean pacsIsAvailable = pacsEntityService.isAvailable(pacs);
//                if(pacsIsAvailable) {
//                    final String path = "/studies/" + studyId;
//                    if (_log.isDebugEnabled()) {
//                        _log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, studyId);
//                    }
//                    if (studyId == null) {
//                        XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript);
//                    } else {
//                        XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, studyId);
//                        XDAT.getConfigService().enable(login, "", DicomEdit.ToolName, path, Scope.Site, studyId);
//                    }
//
//                    ExecutedPacsRequest pacsReq = new ExecutedPacsRequest();
//                    pacsReq.setPacsId(pacsId);
//                    pacsReq.setUsername(user.getUsername());
//                    pacsReq.setXnatProject(project);
//                    pacsReq.setStudyInstanceUid(currStudy.getStudyInstanceUid());
//                    pacsReq.setSeriesIds(_seriesIdsString);
//                    pacsReq.setDestinationAeTitle(ae);
//                    pacsReq.setExecutedTime(new Date());
//
//                    XDAT.getContextService().getBean(ExecutedPacsRequestService.class).create(pacsReq);
//
//                    importFromPacsRequest(pacsReq);
//
//                    final String siteUrl = XDAT.getSiteConfigPreferences().getSiteUrl();
//                    final StringBuilder prearchive = new StringBuilder(siteUrl);
//                    if (!siteUrl.endsWith("/")) {
//                        prearchive.append("/");
//                    }
//                    prearchive.append("app/template/XDATScreen_prearchives.vm");
//
//                    try {
//                        if (_log.isDebugEnabled()) {
//                            _log.debug("Completed DICOM request for study " + currStudy.getStudyInstanceUid() + (StringUtils.isBlank(project) ? " with no project assignment." : " assigned to project " + project));
//                        }
//                        //sendNotification(context, "Selected DICOM series requested", "SeriesRequested");
//                    } catch (Exception exception) {
//                        _log.warn("User " + user.getLogin() + " successfully requested one or more DICOM series, but an error occurred sending the notification email.", exception);
//                    }
//
//                    final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST");
//                    eventDetails.setComment("Series: " + _seriesIdsString);
//                    PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, currStudy.getStudyId(), project, eventDetails);
//                    assert wrk != null;
//                    PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
//                }
//                else{
                    QueuedPacsRequest pacsReq = new QueuedPacsRequest();
                    pacsReq.setPacsId(pacsId);
                    pacsReq.setUsername(user.getUsername());
                    pacsReq.setXnatProject(project);
                    pacsReq.setStudyInstanceUid(currStudy.getStudyInstanceUid());
                    pacsReq.setSeriesIds(_seriesIdsString);
                    pacsReq.setDestinationAeTitle(ae);
                    pacsReq.setQueuedTime(new Date());
                    pacsReq.setRemappingScript(currAnonScript);
                    XDAT.getContextService().getBean(QueuedPacsRequestService.class).create(pacsReq);
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
                if (cause == null || !(cause instanceof Exception)) {
                } else if (cause instanceof CMoveFailureException) {
                    final CMoveFailureException failure = (CMoveFailureException) cause;
                    _log.error("C-MOVE operation failed:\n" + failure.getMessage(), failure);
                }
            }
        }
    }

    protected Study assignStudyToProject(final String projectId, final String studyInstanceUid, final String username) {
        if (!StringUtils.isBlank(projectId)) {
            if (_log.isDebugEnabled()) {
                _log.debug("Assigning study instance UID " + studyInstanceUid + " to project " + projectId);
            }
            XDAT.getContextService().getBean(StudyRoutingService.class).assign(studyInstanceUid, projectId, username);
            return new Study(projectId, studyInstanceUid);
        } else {
            if (_log.isDebugEnabled()) {
                _log.debug("No project assignment specified for study instance UID " + studyInstanceUid + ", may be registered as Unassigned");
            }
            return new Study(studyInstanceUid);
        }
    }

    private static PacsEntityService getPacsEntityService() {
        return XDAT.getContextService().getBean(PacsEntityService.class);
    }

    private static DicomSCPManager getScpManager() {
        return XDAT.getContextService().getBean(DicomSCPManager.class);
    }

    private static ArchiveProcessorInstanceService getProcessorService() {
        return XDAT.getContextService().getBean(ArchiveProcessorInstanceService.class);
    }
}
