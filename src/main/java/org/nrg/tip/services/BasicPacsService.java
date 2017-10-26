/*
 * org.nrg.tip.services.BasicPacsService
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.services;

import javax.inject.Inject;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.ser.DefaultSerializerProvider;
import org.apache.commons.lang.StringUtils;
import org.nrg.config.exceptions.ConfigServiceException;
import org.nrg.config.services.SiteConfigurationService;
import org.nrg.dcm.scp.DicomSCPManager;
import org.nrg.tip.dicom.command.cfind.CFindSCU;
import org.nrg.tip.dicom.command.cfind.dcm4che.tool.Dcm4cheToolCFindSCU;
import org.nrg.tip.dicom.command.cmove.CMoveSCU;
import org.nrg.tip.dicom.command.cmove.dcm4che.tool.Dcm4cheToolCMoveSCU;
import org.nrg.tip.dicom.command.cstore.BasicCStoreSCU;
import org.nrg.tip.dicom.command.cstore.CStoreSCU;
import org.nrg.tip.dicom.net.DicomConnectionProperties;
import org.nrg.tip.dicom.strategy.orm.OrmStrategy;
import org.nrg.tip.domain.Patient;
import org.nrg.tip.domain.Series;
import org.nrg.tip.domain.Study;
import org.nrg.tip.domain.entities.Pacs;
import org.nrg.tip.dto.PacsSearchCriteria;
import org.nrg.tip.dto.PacsSearchResults;
import org.nrg.tip.restlet.NullValueSerializer;
import org.nrg.tip.util.TipRuntimeException;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatImagescandata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.utils.MethodName;
import org.nrg.xnat.utils.WorkflowUtils;
import org.springframework.stereotype.Service;

@Service
public class BasicPacsService implements PacsService {

    @Inject
    private SiteConfigurationService siteConfigurationService;

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
    public void importSeries(final UserI user, final Pacs pacs, final Study study, final Series series) {
        PersistentWorkflowI workflow = null;
        try {
            workflow = buildOpenWorkflow(
                    user,
                    "pacs:import",
                    pacs.getAeTitle(),
                    null,
                    EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.WEB_SERVICE,
                            MethodName.currentMethodName(), null, MAPPER.writeValueAsString(series)));
            buildCMoveSCU(pacs).cmoveSeries(study, series);
            completeWorkflow(workflow);
        } catch (final Exception e) {
            failWorkflow(workflow);
            throw new RuntimeException(e);
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

    private CFindSCU buildCFindSCU(final Pacs pacs) {
        return new Dcm4cheToolCFindSCU(buildDicomConnectionProperties(pacs), getOrmStrategy(pacs));
    }

    private CMoveSCU buildCMoveSCU(final Pacs pacs) {
        return new Dcm4cheToolCMoveSCU(buildDicomConnectionProperties(pacs), getOrmStrategy(pacs));
    }

    private CStoreSCU buildCStoreSCU(final Pacs pacs) {
        return new BasicCStoreSCU(buildDicomConnectionProperties(pacs));
    }

    private DicomConnectionProperties buildDicomConnectionProperties(final Pacs pacs) {
        // At some point in the future caller will probably specify AE as well
        // For now, this is an ugly hack that just uses the first defined AE in the XNAT webapp
        final String localAETitle = XDAT.getContextService().getBean(DicomSCPManager.class).getDicomSCPInstances()
                .values().iterator().next().getAeTitle();
        return new DicomConnectionProperties(localAETitle, pacs);
    }

    private OrmStrategy getOrmStrategy(final Pacs pacs) {
        try {
            return XDAT.getContextService().getBean(pacs.getOrmStrategySpringBeanId(), OrmStrategy.class);
        } catch (final Exception e) {
            throw new TipRuntimeException(String.format("Failed to load the ORM strategy defined by bean '%s'",
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
            return Boolean.valueOf(StringUtils.trimToEmpty(siteConfigurationService
                    .getSiteConfigurationProperty("tip.leavePacsAuditTrail")));
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
}
