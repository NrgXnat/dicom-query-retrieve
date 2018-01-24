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
import org.nrg.dqr.dicom.command.cfind.CFindSCU;
import org.nrg.dqr.dicom.command.cfind.dcm4che.tool.Dcm4cheToolCFindSCU;
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
import org.nrg.dqr.dto.PacsSearchCriteria;
import org.nrg.dqr.dto.PacsSearchResults;
import org.nrg.dqr.restlet.NullValueSerializer;
import org.nrg.dqr.util.DqrRuntimeException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.restlet.extensions.*;
import org.nrg.xnat.utils.MethodName;
import org.nrg.xnat.utils.WorkflowUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.Collection;
import java.util.List;

@Service
public class BasicPacsService implements PacsService {

    private static final Logger _log = LoggerFactory.getLogger(BasicPacsService.class);

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
        PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
        Pacs pacs = pacsEntityService.retrieve(request.getPacsId());
        if(!pacs.isQueryable()) {
            throw new PacsNotQueryableException();
        }
        else if(!aeIsStorable(request.getDestinationAeTitle())){
            throw new PacsNotStorableException();
        }
        else {
            try {

                final Study study = assignStudyToProject(request.getXnatProject(), request.getStudyId(), request.getUsername());

                for (String seriesId : Arrays.asList(request.getSeriesIds().split(","))) {
                    seriesId = seriesId.trim();
                    if (_log.isDebugEnabled()) {
                        _log.debug("Requesting series " + seriesId + " for study instance UID " + request.getStudyId());
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
                        buildCMoveSCU(pacs, request.getDestinationAeTitle()).cmoveSeries(study, series);
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

    private CFindSCU buildCFindSCU(final Pacs pacs) throws PacsNotQueryableException {
        if(!pacs.isQueryable()){
            throw new PacsNotQueryableException();
        }
        return new Dcm4cheToolCFindSCU(buildDicomConnectionProperties(pacs), getOrmStrategy(pacs));
    }

    private CMoveSCU buildCMoveSCU(final Pacs pacs) {
        return new Dcm4cheToolCMoveSCU(buildDicomConnectionProperties(pacs), getOrmStrategy(pacs));
    }

    private CStoreSCU buildCStoreSCU(final Pacs pacs) {
        return new BasicCStoreSCU(buildDicomConnectionProperties(pacs));
    }

    private CMoveSCU buildCMoveSCU(final Pacs pacs, final String receiverAETitle) {
        return new Dcm4cheToolCMoveSCU(buildDicomConnectionProperties(pacs, receiverAETitle), getOrmStrategy(pacs));
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
        for (DicomSCPInstance scp : scps){
            if(StringUtils.equals(scp.getAeTitle(),ae)){
                return true;
            }
        }

        final List<Pacs> allPacs = XDAT.getContextService().getBean(PacsEntityService.class).findAllStorable();
        for (Pacs pacsToCheck : allPacs){
            if(StringUtils.equals(pacsToCheck.getAeTitle(),ae)){
                return true;
            }
        }
        return false;
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
}
