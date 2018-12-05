package org.nrg.dqr.events;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.nrg.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.services.ExecutedPacsRequestService;
import org.nrg.dqr.services.PacsEntityService;
import org.nrg.dqr.services.PacsService;
import org.nrg.dqr.services.QueuedPacsRequestService;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.restlet.extensions.*;
import org.restlet.data.Status;
import org.springframework.jdbc.core.JdbcTemplate;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.nrg.xnat.task.*;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import lombok.extern.slf4j.Slf4j;

/**
 * Created by mike on 1/23/18.
 */
public class PacsRequestDequeuer extends AbstractXnatRunnable {
    public PacsRequestDequeuer(){
        if (_log.isDebugEnabled()) {
            _log.debug("Initializing the PACS request dequeuer job");
        }
    }

    @Override
    public void runTask() {
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("Executing PACS request dequeuer function");
            }
            PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
            QueuedPacsRequestService queueService = XDAT.getContextService().getBean(QueuedPacsRequestService.class);
            List<QueuedPacsRequest> queue = queueService.getAllOrderedByDate();
            QueuedPacsRequest requestToDequeue = null;
            Pacs pacs = null;
            if(queue!=null){
                for (QueuedPacsRequest req : queue) {
                    pacs = pacsEntityService.retrieve(req.getPacsId());
                    if (pacs.isQueryable() && pacsEntityService.isAvailable(pacs)) {
                        //this is the request to dequeue
                        requestToDequeue = req;
                        break;
                    }
                }
            }
            if (requestToDequeue != null) {
                String login = AdminUtils.getAdminUser().getLogin();
                String studyId = requestToDequeue.getStudyInstanceUid();
                String currAnonScript = requestToDequeue.getRemappingScript();
                final String path = "/studies/" + studyId;
                if (_log.isDebugEnabled()) {
                    _log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, studyId);
                }
                if (studyId == null) {
                    XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript);
                } else {
                    XDAT.getContextService().getBean(StudyRoutingService.class).close(studyId);
                    XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, studyId);
                    XDAT.getConfigService().enable(login, "", DicomEdit.ToolName, path, Scope.Site, studyId);
                }

                ExecutedPacsRequest pacsReq = new ExecutedPacsRequest();
                pacsReq.setPacsId(requestToDequeue.getPacsId());
                String username = requestToDequeue.getUsername();
                XDATUser user = new XDATUser(username);
                pacsReq.setUsername(username);
                String projectId = requestToDequeue.getXnatProject();
                pacsReq.setXnatProject(projectId);
                String studyInstanceUid = requestToDequeue.getStudyInstanceUid();
                pacsReq.setStudyInstanceUid(studyInstanceUid);
                String seriesIds = requestToDequeue.getSeriesIds();
                pacsReq.setSeriesIds(seriesIds);
                pacsReq.setDestinationAeTitle(requestToDequeue.getDestinationAeTitle());
                pacsReq.setExecutedTime(new Date());
                pacsReq.setQueuedTime(requestToDequeue.getQueuedTime());

                XDAT.getContextService().getBean(ExecutedPacsRequestService.class).create(pacsReq);

                PacsService pacsService = XDAT.getContextService().getBean(PacsService.class);
                pacsService.importFromPacsRequest(pacsReq);

                queueService.delete(requestToDequeue.getId());


                final String siteUrl = XDAT.getSiteConfigPreferences().getSiteUrl();
                final StringBuilder prearchive = new StringBuilder(siteUrl);
                if (!siteUrl.endsWith("/")) {
                    prearchive.append("/");
                }
                prearchive.append("app/template/XDATScreen_prearchives.vm");

                final PacsServiceResourceContext context = new PacsServiceResourceContext();
                context.put("prearchive", prearchive.toString());
                context.put("studyId", studyInstanceUid);
                context.put("seriesIds", Arrays.asList(seriesIds.split("\\s*,\\s*")));

                try {
                    if (_log.isDebugEnabled()) {
                        _log.debug("Completed DICOM request for study " + studyInstanceUid + (StringUtils.isBlank(projectId) ? " with no project assignment." : " assigned to project " + projectId));
                    }
                    String subject = "Selected DICOM series requested";
                    String template = "SeriesRequested";
                    final String adminEmail = XDAT.getSiteConfigPreferences().getAdminEmail();
                    context.put("pacs", pacs);
                    context.put("adminEmail", adminEmail);
                    final String body = AdminUtils.populateVmTemplate(context, "/screens/dqr/email/" + template + ".vm");
                    XDAT.getMailService().sendHtmlMessage(adminEmail, user.getEmail(), "[" + TurbineUtils.GetSystemName()+"] " + subject, body);


                } catch (Exception exception) {
                    _log.warn("User " + username + " successfully requested one or more DICOM series, but an error occurred sending the notification email.", exception);
                }

                final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST");
                eventDetails.setComment("Series: " + seriesIds);
                PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, studyInstanceUid, projectId, eventDetails);
                assert wrk != null;
                PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
            }
        } catch (Throwable exception) {
            _log.error("Error executing a PACS request from the queue.", exception);
        }
    }

    private static final Logger _log = LoggerFactory.getLogger(PacsRequestDequeuer.class);
}
