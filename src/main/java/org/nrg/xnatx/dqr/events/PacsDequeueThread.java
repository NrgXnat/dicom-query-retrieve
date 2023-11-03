/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.events.PacsDequeueThread
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.events;

import static java.lang.Math.max;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.nrg.config.services.ConfigService;
import org.nrg.framework.constants.Scope;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.task.AbstractXnatRunnable;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.*;

import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
public class PacsDequeueThread extends AbstractXnatRunnable {
    private static final String PREARCHIVE_SCREEN                          = "app/template/XDATScreen_prearchives.vm";

    public PacsDequeueThread(final Long pacsId, final PacsThreads threads, final DicomQueryRetrieveService dqrService, final PacsService pacsService, final QueuedPacsRequestService queuedPacsRequestService, final ExecutedPacsRequestService executedPacsRequestService, final PacsAvailabilityService pacsAvailabilityService, final StudyRoutingService studyRoutingService, final DqrPreferences dqrPreferences, final SiteConfigPreferences siteConfigPreferences, final ConfigService configService, final MailService mailService, final XnatUserProvider primaryAdminUserProvider) {
        log.debug("Initializing the PACS dequeue thread job");
        _pacsId = pacsId;
        _threads = threads;
        _dqrService = dqrService;
        _pacsService = pacsService;
        _queuedPacsRequestService = queuedPacsRequestService;
        _executedPacsRequestService = executedPacsRequestService;
        _pacsAvailabilityService = pacsAvailabilityService;
        _studyRoutingService = studyRoutingService;
        _dqrPreferences = dqrPreferences;
        _siteConfigPreferences = siteConfigPreferences;
        _configService = configService;
        _mailService = mailService;
        _primaryAdminUserProvider = primaryAdminUserProvider;
    }

    @Override
    public void runTask() {
        try {
            log.debug("Executing PACS dequeue thread function for PACS {}", _pacsId);
            while (true) {
                final Optional<PacsAvailability> getAvailability = _pacsAvailabilityService.findAvailableNow(_pacsId);
                if (!getAvailability.isPresent()) {
                    break;
                }
                final PacsAvailability availability = getAvailability.get();
                if (_threads.isOversubscribed(_pacsId, availability.getThreads())) {
                    synchronized (OVERSUBSCRIBED_THREADS_LOCK) {
                        // Double-checked locking to avoid race condition
                        // Maybe this thread was blocked at the sync point for a while, and in the meantime another
                        //  thread has already stopped itself and we are no longer oversubscribed.
                        if (_threads.isOversubscribed(_pacsId, availability.getThreads())) {
                            log.debug("PACS {} is oversubscribed. Stopping thread.", _pacsId);
                            break;
                        }
                    }
                }
                final UserI admin      = _primaryAdminUserProvider.get();
                boolean     canConnect = _dqrService.canConnect(admin, _pacsService.retrieve(_pacsId));
                if (!canConnect) {
                    break;
                }

                boolean failed = false;
                final String priorStatus;//maintain prior status in case the operation fails and we need to roll back.
                final QueuedPacsRequest request;
                synchronized (QUEUE_LOCK) {
                    final List<QueuedPacsRequest> requests = _queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(_pacsId);
                    if (requests.isEmpty()) {
                        break;
                    }
                    request = requests.get(0);
                    priorStatus=request.getStatus();
                    request.setStatus(PacsRequest.PROCESSING_STATUS_TEXT);
                    _queuedPacsRequestService.update(request);
                }

                final AtomicLong   requestTimeInMilliseconds = new AtomicLong();
                final String       studyInstanceUid          = request.getStudyInstanceUid();
                final List<String> seriesIds                 = request.getSeriesIds();
                final String       projectId                 = request.getXnatProject();
                final String       username                  = request.getUsername();
                final UserI        user                      = Users.getUser(username);


                final ExecutedPacsRequest pacsRequest = ExecutedPacsRequest.builder()
                                                                           .pacsId(_pacsId)
                                                                           .username(username)
                                                                           .xnatProject(projectId)
                                                                           .studyInstanceUid(studyInstanceUid)
                                                                           .seriesIds(seriesIds)
                                                                           .destinationAeTitle(request.getDestinationAeTitle())
                                                                           .status(PacsRequest.ISSUED_STATUS_TEXT)
                                                                           .executedTime(new Date())
                                                                           .queuedTime(request.getQueuedTime())
                                                                           .studyDate(request.getStudyDate())
                                                                           .studyId(request.getStudyId())
                                                                           .accessionNumber(request.getAccessionNumber())
                                                                           .patientName(request.getPatientName())
                                                                           .requestId(request.getRequestId())
                                                                           .build();
                try {
                    final String adminUsername = admin.getUsername();
                    final String studyId       = request.getStudyInstanceUid();
                    final String anonScript    = request.getRemappingScript();
                    final String path          = "/studies/" + studyId;
                    log.debug("User {} is setting {} script for project {}", adminUsername, DicomEdit.ToolName, studyId);
                    if (anonScript != null) {
                        if (studyId == null) {
                            _configService.replaceConfig(adminUsername, "", DicomEdit.ToolName, path, anonScript);
                        } else {
                            _studyRoutingService.close(studyId);
                            _configService.replaceConfig(adminUsername, "", DicomEdit.ToolName, path, anonScript, Scope.Site, studyId);
                            _configService.enable(adminUsername, "", DicomEdit.ToolName, path, Scope.Site, studyId);
                        }
                    }

                    _executedPacsRequestService.create(pacsRequest);

                    final StopWatch stopWatch = StopWatch.createStarted();
                    try {
                        _dqrService.importFromPacsRequest(pacsRequest);
                    } catch (DqrRuntimeException e) {
                        // Rethrow as non-runtime exception so we can catch it and retry
                        throw new Exception(e);
                    } finally {
                        stopWatch.stop();
                        requestTimeInMilliseconds.set(stopWatch.getTime());
                    }

                    request.setStatus(PacsRequest.ISSUED_STATUS_TEXT);
                    _queuedPacsRequestService.update(request);

                    closeRequest(request);
                } catch (Exception e) {
                    log.error("REQ {} - Error executing PACS import request.", request.getId(), e);
                    failed = true;

                    pacsRequest.setStatus(PacsRequest.FAILED_STATUS_TEXT);
                    pacsRequest.setErrorMessage(e.getMessage());
                    _executedPacsRequestService.update(pacsRequest);

                    final Integer attempts = Optional.ofNullable(request.getRetries()).orElse(0) + 1;

                    final Integer maxAttempts = Integer.parseInt(_dqrPreferences.getDqrMaxPacsCMOVEAttempts());

                    if (attempts < maxAttempts){
                        log.debug("REQ {} - Retry CMOVE request ({} of {} attempts)", request.getId(), attempts, maxAttempts);

                        //rollback to prior state
                        request.setRetries(attempts);
                        request.setStatus(priorStatus);
                        _queuedPacsRequestService.update(request);

                        try {
                            final Long secondsBeforeRetry = Long.parseLong(_dqrPreferences.getDqrWaitToRetryCMOVETimeInSeconds());
                            long sleepTimeSeconds = TimeUnit.SECONDS.convert(calculateTimeToSleepBasedOnLoad(requestTimeInMilliseconds, availability), TimeUnit.MICROSECONDS);
                            sleepTimeSeconds = max(secondsBeforeRetry, sleepTimeSeconds);
                            TimeUnit.SECONDS.sleep(sleepTimeSeconds);
                            log.debug("PACS {} - Slept for {}s", _pacsId, sleepTimeSeconds);
                        } catch (InterruptedException ex) {
                            Thread.currentThread().interrupt();
                            throw new NrgServiceRuntimeException("Thread interrupted while performing pacs operation.", ex);
                        }

                        return;
                    } else {
                        log.debug("REQ {} - Failing CMOVE request after {} attempts", request.getId(), attempts);
                        closeRequest(request);
                    }
                }

                if (StringUtils.isBlank(projectId)) {
                    log.debug("Completed DICOM request for study {} with no project assignment.", studyInstanceUid);
                } else {
                    log.debug("Completed DICOM request for study {}  assigned to project {}.", studyInstanceUid, projectId);
                }

                if (!failed) {
                    //this seemed like it was notifying success no matter what
                    sendNotification(user, seriesIds);
                }

                saveWorkflowEntry(user,seriesIds,studyInstanceUid,projectId,failed);

                final long sleepTimeMicroseconds = calculateTimeToSleepBasedOnLoad(requestTimeInMilliseconds, availability);
                TimeUnit.MICROSECONDS.sleep(sleepTimeMicroseconds);
                log.debug("PACS {} - Slept for {} µs", _pacsId, sleepTimeMicroseconds);
            }
        } catch (Throwable exception) {
            log.error("Error executing a request for PACS {} from the queue.", _pacsId, exception);
        } finally {
            log.debug("Ending PacsDequeueThread for PACS {}", _pacsId);
            _threads.decrement(_pacsId);
        }
    }

    private long calculateTimeToSleepBasedOnLoad(final AtomicLong   requestTimeInMilliseconds, final PacsAvailability availability){
        final long         requestTimeMicroseconds   = requestTimeInMilliseconds.get() * 1000;
        final long         sleepTimeMicroseconds     = (long) ((((double) 100 / (double) availability.getUtilizationPercent()) - 1) * requestTimeMicroseconds);
        final long         effectiveUtilizationPercent = (long) (100.0d * requestTimeMicroseconds / (sleepTimeMicroseconds + requestTimeMicroseconds));

        log.debug("PACS {} - Ran for {} µs. Sleeping for {} µs. Effective utilization {}%",
            _pacsId, requestTimeMicroseconds, sleepTimeMicroseconds, effectiveUtilizationPercent);

        return sleepTimeMicroseconds;
    }

    private void closeRequest(final QueuedPacsRequest request){
        try {
            _queuedPacsRequestService.delete(request.getId());
        } catch (Exception ex) {
            log.error("Error removing PACS import request from queue.", ex);
        }
    }

    private void sendNotification(final UserI user, final List<String> seriesIds){
        final Context context = new VelocityContext();
        context.put("prearchive", StringUtils.appendIfMissing(_siteConfigPreferences.getSiteUrl(), "/") + PREARCHIVE_SCREEN);
        context.put("seriesIds", seriesIds);

        try {

            final String adminEmail = _siteConfigPreferences.getAdminEmail();
            context.put("adminEmail", adminEmail);
            context.put("pacs", _pacsService.retrieve(_pacsId));
            if (_dqrPreferences.getNotifyAdminOnImport()) {
                _mailService.sendHtmlMessage(adminEmail, user.getEmail(), adminEmail, String.format(SUBJECT_FORMAT, seriesIds.size()), AdminUtils.populateVmTemplate(context, "/screens/dqr/email/SeriesRequested.vm"));
            }
        } catch (Exception exception) {
            log.warn("User {} requested one or more DICOM series, but an error occurred sending the notification email.", user.getUsername(), exception);
        }
    }

    public void saveWorkflowEntry(final UserI user, final List<String> seriesIds, final String studyInstanceUid, final String projectId, final boolean failed) throws Exception {
        final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST");
        eventDetails.setComment("Series: " + seriesIds);
        PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, studyInstanceUid, projectId, eventDetails);
        assert wrk != null;
        if(failed){
            PersistentWorkflowUtils.fail(wrk, wrk.buildEvent());
        }else{
            PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
        }
    }

    private final static Object QUEUE_LOCK     = new Object();
    private final static Object OVERSUBSCRIBED_THREADS_LOCK     = new Object();
    private final static String SUBJECT_FORMAT = "[" + TurbineUtils.GetSystemName() + "] %d selected DICOM series requested";

    private final Long                       _pacsId;
    private final PacsThreads               _threads;
    private final DicomQueryRetrieveService _dqrService;
    private final PacsService               _pacsService;
    private final QueuedPacsRequestService  _queuedPacsRequestService;
    private final ExecutedPacsRequestService _executedPacsRequestService;
    private final PacsAvailabilityService    _pacsAvailabilityService;
    private final StudyRoutingService        _studyRoutingService;
    private final DqrPreferences             _dqrPreferences;
    private final SiteConfigPreferences      _siteConfigPreferences;
    private final ConfigService              _configService;
    private final MailService                _mailService;
    private final XnatUserProvider           _primaryAdminUserProvider;
}
