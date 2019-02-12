package org.nrg.dqr.events;

import com.google.common.base.Joiner;
import org.apache.commons.lang.StringUtils;
import org.nrg.dqr.dicom.command.cmove.CMoveFailureException;
import org.nrg.dqr.dicom.command.cmove.CMoveTargetNotFoundException;
import org.nrg.dqr.domain.entities.*;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.*;
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

import java.util.*;

import lombok.extern.slf4j.Slf4j;

import javax.inject.Inject;

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
            PacsService pacsService = XDAT.getContextService().getBean(PacsService.class);
            PacsAvailabilityEntityService pacsAvailabilityEntityService = XDAT.getContextService().getBean(PacsAvailabilityEntityService.class);
            QueuedPacsRequestService queueService = XDAT.getContextService().getBean(QueuedPacsRequestService.class);
            ExecutedPacsRequestService executedService = XDAT.getContextService().getBean(ExecutedPacsRequestService.class);
            List<QueuedPacsRequest> requestsToDequeue = new ArrayList<>();

            List<Pacs> pacsList = pacsEntityService.findAllQueryable();
            if(pacsList!=null) {
                for (Pacs currPacs : pacsList) {
                    try {
                        Long pacsId = currPacs.getId();
                        Integer defaultDequeuesPerHour = currPacs.getDefaultDequeuesPerHour();
                        Integer sessionsPerDequeue = currPacs.getDefaultSessionsPerDequeue();
                        if (defaultDequeuesPerHour != null && sessionsPerDequeue != null) {
                            Long millisBetweenPacsRequests = 0L;
                            if (defaultDequeuesPerHour != 0L) {
                                millisBetweenPacsRequests = (3600000L / defaultDequeuesPerHour);
                            }
                            List<PacsAvailability> availabilityList = pacsAvailabilityEntityService.findSettingsByPacs(pacsId);
                            for (PacsAvailability availability : availabilityList) {
                                String availabilityStartTimeString = availability.getAvailabilityStart();
                                String availabilityEndTimeString = availability.getAvailabilityEnd();
                                int availabilityDay = availability.getDayOfWeek();

                                //If hour is one digit, pad with a zero.
                                if (availabilityStartTimeString.charAt(1) == ':') {
                                    availabilityStartTimeString = "0" + availabilityStartTimeString;
                                }
                                if (availabilityEndTimeString.charAt(1) == ':') {
                                    availabilityEndTimeString = "0" + availabilityEndTimeString;
                                }
                                Calendar currentCal = Calendar.getInstance();
                                int currentDayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK);

                                long currMillis = currentCal.getTimeInMillis();

                                long startMillis = 0L;
                                long endMillis = 0L;

                                if (StringUtils.isNotBlank(availabilityStartTimeString)) {
                                    try {
                                        Calendar startCal = (Calendar) currentCal.clone();
                                        String[] startTime = StringUtils.split(availabilityStartTimeString, ":");
                                        startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTime[0]));
                                        startCal.set(Calendar.MINUTE, Integer.parseInt(startTime[1]));
                                        startMillis = startCal.getTimeInMillis();
                                    } catch (Exception e) {

                                    }
                                }
                                if (StringUtils.isNotBlank(availabilityEndTimeString)) {
                                    try {
                                        Calendar endCal = (Calendar) currentCal.clone();
                                        String[] endTime = StringUtils.split(availabilityEndTimeString, ":");
                                        endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTime[0]));
                                        endCal.set(Calendar.MINUTE, Integer.parseInt(endTime[1]));
                                        endMillis = endCal.getTimeInMillis();
                                    } catch (Exception e) {

                                    }
                                }

                                boolean isAvailable = false;
                                if (endMillis < startMillis) {
                                    //That means that the availability interval contains midnight.
                                    if ((currMillis > startMillis && currentDayOfWeek == availabilityDay) || (currMillis < endMillis && currentDayOfWeek == (availabilityDay + 1))) {
                                        isAvailable = true;
                                    }
                                } else {
                                    if (currMillis > startMillis && currMillis < endMillis && currentDayOfWeek == availabilityDay) {
                                        isAvailable = true;
                                    }
                                }

                                if (isAvailable) {
                                    long sessionsPerHour = availability.getDequeuesPerHour();
                                    if (sessionsPerHour == 0L) {
                                        millisBetweenPacsRequests = 0L;

                                    } else {
                                        millisBetweenPacsRequests = (3600000 / sessionsPerHour);
                                    }
                                    sessionsPerDequeue = availability.getSessionsPerDequeue();
                                    break;
                                }
                            }
                            if (millisBetweenPacsRequests != 0L && sessionsPerDequeue>0) {
                                ExecutedPacsRequest lastReq = executedService.getMostRecentForPacs(pacsId);
                                if (lastReq != null) {
                                    Date executedTime = lastReq.getExecutedTime();
                                    Date currTime = new Date();

                                    if (executedTime != null) {
                                        if ((currTime.getTime() - executedTime.getTime()) > (millisBetweenPacsRequests)) {
                                            List<QueuedPacsRequest> reqs = queueService.getAllForPacsOrderedByPriorityAndDate(pacsId);
                                            if (reqs != null && reqs.size() > 0) {

                                                //Try to dequeue requests as long as PACS is responding to pings.
                                                boolean canConnect = pacsService.canConnect(AdminUtils.getAdminUser(), pacsEntityService.retrieve(pacsId));
                                                if (canConnect) {
                                                    int added = 0;
                                                    for (QueuedPacsRequest req : reqs) {
                                                        requestsToDequeue.add(req);
                                                        added++;
                                                        if (added >= sessionsPerDequeue) {
                                                            break;
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                } else {
                                    List<QueuedPacsRequest> reqs = queueService.getAllForPacsOrderedByPriorityAndDate(pacsId);
                                    if (reqs != null && reqs.size() > 0) {

                                        //Try to dequeue requests as long as PACS is responding to pings.
                                        boolean canConnect = pacsService.canConnect(AdminUtils.getAdminUser(), pacsEntityService.retrieve(pacsId));
                                        if (canConnect) {
                                            int added = 0;
                                            for (QueuedPacsRequest req : reqs) {
                                                requestsToDequeue.add(req);
                                                added++;
                                                if (added >= sessionsPerDequeue) {
                                                    break;
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                    catch(Exception e){
                        _log.error("Error getting requests to dequeue for PACS "+currPacs.getId()+".");
                    }
                }
            }
            if (requestsToDequeue != null && requestsToDequeue.size()>0) {
                for(QueuedPacsRequest requestToDequeue: requestsToDequeue) {
                    String studyInstanceUid = "";
                    String seriesIds = "";
                    String projectId = "";
                    String username = "";
                    XDATUser user = new XDATUser();
                    ExecutedPacsRequest pacsReq = new ExecutedPacsRequest();
                    try {
                        String login = AdminUtils.getAdminUser().getLogin();
                        String studyId = requestToDequeue.getStudyInstanceUid();
                        String currAnonScript = requestToDequeue.getRemappingScript();
                        final String path = "/studies/" + studyId;
                        if (_log.isDebugEnabled()) {
                            _log.debug("User {} is setting {} script for project {}", login, DicomEdit.ToolName, studyId);
                        }
                        if (currAnonScript != null) {
                            if (studyId == null) {
                                XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript);
                            } else {
                                XDAT.getContextService().getBean(StudyRoutingService.class).close(studyId);
                                XDAT.getConfigService().replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, studyId);
                                XDAT.getConfigService().enable(login, "", DicomEdit.ToolName, path, Scope.Site, studyId);
                            }
                        }
                        pacsReq.setPacsId(requestToDequeue.getPacsId());
                        username = requestToDequeue.getUsername();
                        user = new XDATUser(username);
                        pacsReq.setUsername(username);
                        projectId = requestToDequeue.getXnatProject();
                        pacsReq.setXnatProject(projectId);
                        studyInstanceUid = requestToDequeue.getStudyInstanceUid();
                        pacsReq.setStudyInstanceUid(studyInstanceUid);
                        seriesIds = requestToDequeue.getSeriesIds();
                        pacsReq.setSeriesIds(seriesIds);
                        pacsReq.setDestinationAeTitle(requestToDequeue.getDestinationAeTitle());
                        pacsReq.setStatus(PacsRequest.ISSUED_STATUS_TEXT);
                        pacsReq.setExecutedTime(new Date());
                        pacsReq.setQueuedTime(requestToDequeue.getQueuedTime());

                        XDAT.getContextService().getBean(ExecutedPacsRequestService.class).create(pacsReq);

                        pacsService.importFromPacsRequest(pacsReq);
                        requestToDequeue.setStatus(PacsRequest.ISSUED_STATUS_TEXT);
                        queueService.update(requestToDequeue);
                    }
                    catch(Exception e){
                        requestToDequeue.setStatus(PacsRequest.FAILED_STATUS_TEXT);
                        queueService.update(requestToDequeue);

                        pacsReq.setStatus(PacsRequest.FAILED_STATUS_TEXT);
                        executedService.update(pacsReq);
                        _log.error("Error executing PACS import request.",e);
                    }
                    finally {
                        try {
                            queueService.delete(requestToDequeue.getId());
                        }
                        catch(Exception e){
                            _log.error("Error removing PACS import request from queue.",e);
                        }
                    }

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
                        DqrPreferences preferences = XDAT.getContextService().getBean(DqrPreferences.class);
                        if(preferences!=null && preferences.getNotifyAdminOnImport()) {
                            String subject = "Selected DICOM series requested";
                            String template = "SeriesRequested";
                            final String adminEmail = XDAT.getSiteConfigPreferences().getAdminEmail();
                            context.put("adminEmail", adminEmail);
                            context.put("pacs", pacsEntityService.retrieve(pacsReq.getPacsId()));
                            final String body = AdminUtils.populateVmTemplate(context, "/screens/dqr/email/" + template + ".vm");
                            XDAT.getMailService().sendHtmlMessage(adminEmail, user.getEmail(), "[" + TurbineUtils.GetSystemName() + "] " + subject, body);
                        }
                    } catch (Exception exception) {
                        _log.warn("User " + username + " requested one or more DICOM series, but an error occurred sending the notification email.", exception);
                    }

                    final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST");
                    eventDetails.setComment("Series: " + seriesIds);
                    PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, studyInstanceUid, projectId, eventDetails);
                    assert wrk != null;
                    PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
                }
            }
        } catch (Throwable exception) {
            _log.error("Error executing a PACS request from the queue.", exception);
        }
    }

    private static final Logger _log = LoggerFactory.getLogger(PacsRequestDequeuer.class);
}
