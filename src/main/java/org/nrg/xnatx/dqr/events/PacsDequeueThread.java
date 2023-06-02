/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.events.PacsDequeueThread
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.apache.velocity.VelocityContext;
import org.apache.velocity.context.Context;
import org.nrg.config.services.ConfigService;
import org.nrg.framework.constants.Scope;
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

    private static final String PREARCHIVE_SCREEN = "app/template/XDATScreen_prearchives.vm";

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
                if (!_threads.hasAvailable(_pacsId, availability.getThreads())) {
                    break;
                }
                final UserI admin      = _primaryAdminUserProvider.get();
                boolean     canConnect = _dqrService.canConnect(admin, _pacsService.retrieve(_pacsId));
                if (!canConnect) {
                    break;
                }

                final QueuedPacsRequest request;
                synchronized (QUEUE_LOCK) {
                    final List<QueuedPacsRequest> requests = _queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(_pacsId);
                    if (requests.isEmpty()) {
                        break;
                    }
                    request = requests.get(0);
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
                                                                           .pacsId(request.getPacsId())
                                                                           .patientName(request.getPatientName())
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
                    _dqrService.importFromPacsRequest(user, pacsRequest);
                    stopWatch.stop();
                    requestTimeInMilliseconds.set(stopWatch.getTime());

                    request.setStatus(PacsRequest.ISSUED_STATUS_TEXT);
                    _queuedPacsRequestService.update(request);
                } catch (Exception e) {
                    request.setStatus(PacsRequest.FAILED_STATUS_TEXT);
                    _queuedPacsRequestService.update(request);

                    pacsRequest.setStatus(PacsRequest.FAILED_STATUS_TEXT);
                    _executedPacsRequestService.update(pacsRequest);
                    log.error("Error executing PACS import request.", e);
                } finally {
                    try {
                        _queuedPacsRequestService.delete(request.getId());
                    } catch (Exception e) {
                        log.error("Error removing PACS import request from queue.", e);
                    }
                }

                final Context context = new VelocityContext();
                context.put("prearchive", StringUtils.appendIfMissing(_siteConfigPreferences.getSiteUrl(), "/") + PREARCHIVE_SCREEN);
                context.put("seriesIds", seriesIds);

                try {
                    if (StringUtils.isBlank(projectId)) {
                        log.debug("Completed DICOM request for study {} with no project assignment.", studyInstanceUid);
                    } else {
                        log.debug("Completed DICOM request for study {}  assigned to project {}.", studyInstanceUid, projectId);
                    }
                    final String adminEmail = _siteConfigPreferences.getAdminEmail();
                    context.put("adminEmail", adminEmail);
                    context.put("pacs", _pacsService.retrieve(_pacsId));
                    if (_dqrPreferences.getNotifyAdminOnImport()) {
                        _mailService.sendHtmlMessage(adminEmail, user.getEmail(), adminEmail, String.format(SUBJECT_FORMAT, seriesIds.size()), AdminUtils.populateVmTemplate(context, "/screens/dqr/email/SeriesRequested.vm"));
                    } else {
                        _mailService.sendHtmlMessage(adminEmail, user.getEmail(), String.format(SUBJECT_FORMAT, seriesIds.size()), AdminUtils.populateVmTemplate(context, "/screens/dqr/email/SeriesRequested.vm"));
                    }
                } catch (Exception exception) {
                    log.warn("User {} requested one or more DICOM series, but an error occurred sending the notification email.", username, exception);
                }

                final EventDetails eventDetails = EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST");
                eventDetails.setComment("Series: " + seriesIds);
                PersistentWorkflowI wrk = PersistentWorkflowUtils.buildOpenWorkflow(user, XnatMrsessiondata.SCHEMA_ELEMENT_NAME, studyInstanceUid, projectId, eventDetails);
                assert wrk != null;
                PersistentWorkflowUtils.complete(wrk, wrk.buildEvent());
                TimeUnit.MICROSECONDS.sleep((long) ((((double) 100 / (double) availability.getUtilizationPercent()) - 1) * requestTimeInMilliseconds.get() * 1000));

                //sync number of thread checks so we dont close too many
                //check current threads for pacs and if there aren't too many running, pull another study from pacs
                //make syncing pacs specific instead of over all pacs
                //...
            }
        } catch (Throwable exception) {
            log.error("Error executing a PACS request from the queue.", exception);
        } finally {
            _threads.remove(_pacsId);
        }
    }

    private final static Object QUEUE_LOCK     = new Object();
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
