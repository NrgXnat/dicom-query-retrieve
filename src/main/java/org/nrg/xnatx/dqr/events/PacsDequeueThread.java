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

import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
public class PacsDequeueThread extends AbstractXnatRunnable {
    public PacsDequeueThread(final Long pacsId, final PacsThreads threads, final PacsService pacsService, final PacsEntityService pacsEntityService, final QueuedPacsRequestService queuedPacsRequestService, final ExecutedPacsRequestService executedPacsRequestService, final PacsAvailabilityEntityService pacsAvailabilityEntityService, final StudyRoutingService studyRoutingService, final DqrPreferences dqrPreferences, final SiteConfigPreferences siteConfigPreferences, final ConfigService configService, final MailService mailService, final XnatUserProvider primaryAdminUserProvider) {
        log.debug("Initializing the PACS dequeue thread job");
        _pacsId = pacsId;
        _threads = threads;
        _pacsService = pacsService;
        _pacsEntityService = pacsEntityService;
        _queuedPacsRequestService = queuedPacsRequestService;
        _executedPacsRequestService = executedPacsRequestService;
        _pacsAvailabilityEntityService = pacsAvailabilityEntityService;
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
            log.debug("Executing PACS dequeue thread function");
            boolean continueThread = true;
            while (continueThread) {
                final PacsAvailability availability = _pacsAvailabilityEntityService.findAvailableNow(_pacsId);
                if (availability != null && _threads.hasAvailable(_pacsId, availability.getThreads())) {
                    final int         utilizationPercent = availability.getUtilizationPercent();
                    QueuedPacsRequest requestToDequeue   = null;
                    final UserI       admin              = _primaryAdminUserProvider.get();
                    boolean           canConnect         = _pacsService.canConnect(admin, _pacsEntityService.retrieve(_pacsId));

                    if (canConnect) {
                        synchronized (QUEUE_LOCK) {
                            List<QueuedPacsRequest> reqs = _queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(_pacsId);
                            if (reqs.isEmpty()) {
                                continueThread = false;//If there are no more requests for this PACS, close the thread
                            } else {
                                requestToDequeue = reqs.get(0);
                                requestToDequeue.setStatus(PacsRequest.PROCESSING_STATUS_TEXT);
                                _queuedPacsRequestService.update(requestToDequeue);
                            }
                        }

                        if (requestToDequeue != null) {
                            long                      requestTimeInMilliseconds = 0L;
                            final String              studyInstanceUid          = requestToDequeue.getStudyInstanceUid();
                            final List<String>        seriesIds                 = requestToDequeue.getSeriesIds();
                            final String              projectId                 = requestToDequeue.getXnatProject();
                            final String              username                  = requestToDequeue.getUsername();
                            final UserI               user                      = Users.getUser(username);
                            final ExecutedPacsRequest pacsRequest               = new ExecutedPacsRequest();
                            try {
                                String       adminUsername  = admin.getUsername();
                                String       studyId        = requestToDequeue.getStudyInstanceUid();
                                String       currAnonScript = requestToDequeue.getRemappingScript();
                                final String path           = "/studies/" + studyId;
                                log.debug("User {} is setting {} script for project {}", adminUsername, DicomEdit.ToolName, studyId);
                                if (currAnonScript != null) {
                                    if (studyId == null) {
                                        _configService.replaceConfig(adminUsername, "", DicomEdit.ToolName, path, currAnonScript);
                                    } else {
                                        _studyRoutingService.close(studyId);
                                        _configService.replaceConfig(adminUsername, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, studyId);
                                        _configService.enable(adminUsername, "", DicomEdit.ToolName, path, Scope.Site, studyId);
                                    }
                                }
                                pacsRequest.setPacsId(_pacsId);
                                pacsRequest.setUsername(username);
                                pacsRequest.setXnatProject(projectId);
                                pacsRequest.setStudyInstanceUid(studyInstanceUid);
                                pacsRequest.setSeriesIds(seriesIds);
                                pacsRequest.setDestinationAeTitle(requestToDequeue.getDestinationAeTitle());
                                pacsRequest.setStatus(PacsRequest.ISSUED_STATUS_TEXT);
                                pacsRequest.setExecutedTime(new Date());
                                pacsRequest.setQueuedTime(requestToDequeue.getQueuedTime());
                                pacsRequest.setStudyDate(requestToDequeue.getStudyDate());
                                pacsRequest.setStudyId(requestToDequeue.getStudyId());
                                pacsRequest.setAccessionNumber(requestToDequeue.getAccessionNumber());
                                pacsRequest.setPacsId(requestToDequeue.getPacsId());
                                pacsRequest.setPatientName(requestToDequeue.getPatientName());

                                _executedPacsRequestService.create(pacsRequest);

                                long startTime = Calendar.getInstance().getTimeInMillis();
                                _pacsService.importFromPacsRequest(pacsRequest);
                                long endTime = Calendar.getInstance().getTimeInMillis();
                                requestTimeInMilliseconds = endTime - startTime;

                                requestToDequeue.setStatus(PacsRequest.ISSUED_STATUS_TEXT);
                                _queuedPacsRequestService.update(requestToDequeue);
                            } catch (Exception e) {
                                requestToDequeue.setStatus(PacsRequest.FAILED_STATUS_TEXT);
                                _queuedPacsRequestService.update(requestToDequeue);

                                pacsRequest.setStatus(PacsRequest.FAILED_STATUS_TEXT);
                                _executedPacsRequestService.update(pacsRequest);
                                log.error("Error executing PACS import request.", e);
                            } finally {
                                try {
                                    _queuedPacsRequestService.delete(requestToDequeue.getId());
                                } catch (Exception e) {
                                    log.error("Error removing PACS import request from queue.", e);
                                }
                            }

                            final String        siteUrl    = _siteConfigPreferences.getSiteUrl();
                            final StringBuilder prearchive = new StringBuilder(siteUrl);
                            if (!siteUrl.endsWith("/")) {
                                prearchive.append("/");
                            }
                            prearchive.append("app/template/XDATScreen_prearchives.vm");

                            final Context context = new VelocityContext();
                            context.put("prearchive", prearchive.toString());
                            context.put("seriesIds", seriesIds);

                            try {
                                log.debug("Completed DICOM request for study {} {}", studyInstanceUid, StringUtils.isBlank(projectId) ? " with no project assignment." : " assigned to project " + projectId);
                                final String adminEmail = _siteConfigPreferences.getAdminEmail();
                                context.put("adminEmail", adminEmail);
                                context.put("pacs", _pacsEntityService.retrieve(_pacsId));
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
                            TimeUnit.MICROSECONDS.sleep((long) ((((double) 100 / (double) utilizationPercent) - 1) * requestTimeInMilliseconds * 1000));
                        }
                    } else {
                        continueThread = false;
                    }

                    //sync number of thread checks so we dont close too many
                    //check current threads for pacs and if there aren't too many running, pull another study from pacs
                    //make syncing pacs specific instead of over all pacs
                    //...
                } else {
                    continueThread = false;
                }
            }
        } catch (Throwable exception) {
            log.error("Error executing a PACS request from the queue.", exception);
        } finally {
            _threads.remove(_pacsId);
        }
    }

    private final static Object QUEUE_LOCK     = new Object();
    private final static String SUBJECT_FORMAT = "[" + TurbineUtils.GetSystemName() + "] %d selected DICOM series requested";

    private final Long                          _pacsId;
    private final PacsThreads                   _threads;
    private final PacsService                   _pacsService;
    private final PacsEntityService             _pacsEntityService;
    private final QueuedPacsRequestService      _queuedPacsRequestService;
    private final ExecutedPacsRequestService    _executedPacsRequestService;
    private final PacsAvailabilityEntityService _pacsAvailabilityEntityService;
    private final StudyRoutingService           _studyRoutingService;
    private final DqrPreferences                _dqrPreferences;
    private final SiteConfigPreferences         _siteConfigPreferences;
    private final ConfigService                 _configService;
    private final MailService                   _mailService;
    private final XnatUserProvider              _primaryAdminUserProvider;
}
