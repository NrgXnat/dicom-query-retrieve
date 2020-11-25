package org.nrg.dqr.events;

import org.apache.commons.lang3.StringUtils;
import org.nrg.dqr.domain.entities.*;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.*;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.restlet.extensions.PacsServiceResourceContext;
import org.nrg.xnat.task.AbstractXnatRunnable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.DayOfWeek;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike on 1/23/18.
 */
public class PacsDequeueThread extends AbstractXnatRunnable {
    private final static Object THREAD_COUNT_LOCK = new Object();
    private final static Object QUEUE_LOCK = new Object();
    public static Map<Long,Integer> currentThreadsPerPacs = new HashMap<>();
    private Long pacsId;
    private PacsAvailabilityEntityService pacsAvailabilityEntityService;
    private QueuedPacsRequestService queueService;
    private PacsService pacsService;
    private PacsEntityService pacsEntityService;
    private ExecutedPacsRequestService executedService;

    public PacsDequeueThread(Long pacsId, PacsAvailabilityEntityService pacsAvailabilityEntityService, QueuedPacsRequestService queueService, PacsService pacsService, PacsEntityService pacsEntityService, ExecutedPacsRequestService executedService){
        if (_log.isDebugEnabled()) {
            _log.debug("Initializing the PACS dequeue thread job");
        }
        synchronized (THREAD_COUNT_LOCK) {
            Integer currThreads = currentThreadsPerPacs.get(pacsId);
            if(currThreads==null){
                currentThreadsPerPacs.put(pacsId,1);
            }
            else{
                currentThreadsPerPacs.put(pacsId,currThreads+1);
            }
        }
        this.pacsAvailabilityEntityService = pacsAvailabilityEntityService;
        this.queueService = queueService;
        this.pacsService = pacsService;
        this.pacsEntityService = pacsEntityService;
        this.executedService = executedService;
        this.pacsId = pacsId;
    }

    @Override
    public void runTask() {
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("Executing PACS dequeue thread function");
            }
            boolean continueThread = true;
            while(continueThread) {
                Calendar currentCal = Calendar.getInstance();
                int currentDayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK);
                List<PacsAvailability> availabilityList = pacsAvailabilityEntityService.findAllByPacsIdAndDayOfWeek(pacsId, DayOfWeek.of(currentDayOfWeek));
                int utilizationPercent = 0;
                int threads = 0;
                for (PacsAvailability availability : availabilityList) {
                    String availabilityStartTimeString = availability.getAvailabilityStart();
                    String availabilityEndTimeString = availability.getAvailabilityEnd();
                    int availabilityDay = availability.getDayOfWeek().getValue();

                    //If hour is one digit, pad with a zero.
                    if (availabilityStartTimeString.charAt(1) == ':') {
                        availabilityStartTimeString = "0" + availabilityStartTimeString;
                    }
                    if (availabilityEndTimeString.charAt(1) == ':') {
                        availabilityEndTimeString = "0" + availabilityEndTimeString;
                    }

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
                        utilizationPercent = availability.getUtilizationPercent();
                        threads = availability.getThreads();
                        break;
                    }
                }
                if (utilizationPercent > 0 && threads > 0 && currentThreadsPerPacs.get(pacsId)<=threads) {
                    QueuedPacsRequest requestToDequeue = null;
                    boolean canConnect = pacsService.canConnect(AdminUtils.getAdminUser(), pacsEntityService.retrieve(pacsId));

                    if (canConnect) {
                        synchronized (QUEUE_LOCK) {
                            List<QueuedPacsRequest> reqs = queueService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(pacsId);
                            if (reqs.isEmpty()) {
                                continueThread = false;//If there are no more requests for this PACS, close the thread
                            } else {
                                requestToDequeue = reqs.get(0);
                                requestToDequeue.setStatus(PacsRequest.PROCESSING_STATUS_TEXT);
                                queueService.update(requestToDequeue);
                            }
                        }

                        if(requestToDequeue!=null) {
                            long requestTimeInMilliseconds = 0L;
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
                                pacsReq.setPacsId(pacsId);
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
                                pacsReq.setStudyDate(requestToDequeue.getStudyDate());
                                pacsReq.setStudyId(requestToDequeue.getStudyId());
                                pacsReq.setAccessionNumber(requestToDequeue.getAccessionNumber());
                                pacsReq.setPacsId(requestToDequeue.getPacsId());
                                pacsReq.setPatientName(requestToDequeue.getPatientName());


                                XDAT.getContextService().getBean(ExecutedPacsRequestService.class).create(pacsReq);

                                long startTime = Calendar.getInstance().getTimeInMillis();
                                pacsService.importFromPacsRequest(pacsReq);
                                long endTime = Calendar.getInstance().getTimeInMillis();
                                requestTimeInMilliseconds = endTime - startTime;


                                requestToDequeue.setStatus(PacsRequest.ISSUED_STATUS_TEXT);
                                queueService.update(requestToDequeue);
                            } catch (Exception e) {
                                requestToDequeue.setStatus(PacsRequest.FAILED_STATUS_TEXT);
                                queueService.update(requestToDequeue);

                                pacsReq.setStatus(PacsRequest.FAILED_STATUS_TEXT);
                                executedService.update(pacsReq);
                                _log.error("Error executing PACS import request.", e);
                            } finally {
                                try {
                                    queueService.delete(requestToDequeue.getId());
                                } catch (Exception e) {
                                    _log.error("Error removing PACS import request from queue.", e);
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
                                if (preferences != null && preferences.getNotifyAdminOnImport()) {
                                    String subject = "Selected DICOM series requested";
                                    String template = "SeriesRequested";
                                    final String adminEmail = XDAT.getSiteConfigPreferences().getAdminEmail();
                                    context.put("adminEmail", adminEmail);
                                    context.put("pacs", pacsEntityService.retrieve(pacsId));

                                    final String body = AdminUtils.populateVmTemplate(context, "/screens/dqr/email/" + template + ".vm");
                                    XDAT.getMailService().sendHtmlMessage(adminEmail, adminEmail, "[" + TurbineUtils.GetSystemName() + "] " + subject, body);
                                }
                            } catch (Exception exception) {
                                _log.warn("User " + username + " requested one or more DICOM series, but an error occurred sending the notification email.", exception);
                            }

                            final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST");
                            eventDetails.setComment("Series: " + seriesIds);
                            PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, studyInstanceUid, projectId, eventDetails);
                            assert wrk != null;
                            PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
                            TimeUnit.MICROSECONDS.sleep((long)((((double)100 / (double)utilizationPercent) - 1) * requestTimeInMilliseconds*1000));
                        }
                    }
                    else{
                        continueThread = false;
                    }

                    //sync number of thread checks so we dont close too many
                    //check current threads for pacs and if there aren't too many running, pull another study from pacs
                    //make syncing pacs specific instead of over all pacs
                    //...
                }
                else{
                    continueThread = false;
                }
            }
        } catch (Throwable exception) {
            _log.error("Error executing a PACS request from the queue.", exception);
        } finally {
            synchronized (THREAD_COUNT_LOCK) {
                Integer currThreads = currentThreadsPerPacs.get(pacsId);
                currentThreadsPerPacs.put(pacsId,currThreads-1);
            }
        }
    }

    private static final Logger _log = LoggerFactory.getLogger(PacsDequeueThread.class);
}
