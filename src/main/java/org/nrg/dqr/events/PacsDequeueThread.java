package org.nrg.dqr.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.nrg.config.services.ConfigService;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.PacsAvailability;
import org.nrg.dqr.domain.entities.PacsRequest;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.*;
import org.nrg.framework.constants.Scope;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.helpers.Users;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.restlet.extensions.PacsServiceResourceContext;
import org.nrg.xnat.task.AbstractXnatRunnable;

import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
public class PacsDequeueThread extends AbstractXnatRunnable {
    public static Map<Long, Integer> currentThreadsPerPacs = new HashMap<>();

    public PacsDequeueThread(final long pacsId, final PacsService pacsService, final PacsEntityService pacsEntityService, final PacsAvailabilityEntityService pacsAvailabilityEntityService, final QueuedPacsRequestService queuedPacsRequestService, final ExecutedPacsRequestService executedPacsRequestService, final StudyRoutingService studyRoutingService, final SiteConfigPreferences siteConfigPreferences, final DqrPreferences dqrPreferences, final ConfigService configService, final MailService mailService, final XnatUserProvider userProvider) {
        log.debug("Initializing the dequeue thread job for PACS {}", pacsId);
        synchronized (THREAD_COUNT_LOCK) {
            currentThreadsPerPacs.merge(pacsId, 1, Integer::sum);
        }
        _pacsId = pacsId;
        _pacsService = pacsService;
        _pacsEntityService = pacsEntityService;
        _pacsAvailabilityEntityService = pacsAvailabilityEntityService;
        _queuedPacsRequestService = queuedPacsRequestService;
        _executedPacsRequestService = executedPacsRequestService;
        _studyRoutingService = studyRoutingService;
        _siteConfigPreferences = siteConfigPreferences;
        _dqrPreferences = dqrPreferences;
        _configService = configService;
        _mailService = mailService;
        _userProvider = userProvider;
        _emailSubject = "[" + TurbineUtils.GetSystemName() + "] Selected DICOM series requested";
        _prearchiveUrl = StringUtils.appendIfMissing(_siteConfigPreferences.getSiteUrl(), "/") + "app/template/XDATScreen_prearchives.vm";
    }

    @Override
    public void runTask() {
        try {
            log.debug("Executing PACS dequeue thread function for PACS {}", _pacsId);
            final AtomicBoolean continueThread     = new AtomicBoolean(true);
            final int           currentThreadCount = currentThreadsPerPacs.get(_pacsId);
            while (continueThread.get()) {
                final Optional<PacsAvailability> found = _pacsAvailabilityEntityService.findAllByPacsIdAndDayOfWeek(_pacsId, LocalDateTime.now().getDayOfWeek()).stream().filter(entity -> entity.isAvailableNow() && entity.getUtilizationPercent() > 0 && entity.getThreads() >= currentThreadCount).findAny();
                if (found.isPresent()) {
                    synchronized (QUEUE_LOCK) {
                        final List<QueuedPacsRequest> requests = _queuedPacsRequestService.getQueuedOrFailedForPacsOrderedByPriorityAndDate(_pacsId);
                        if (_pacsService.canConnect(_userProvider.get(), _pacsEntityService.retrieve(_pacsId)) && !requests.isEmpty()) {
                            dequeueRequest(found.get().getUtilizationPercent(), requests);
                        } else {
                            continueThread.set(false);
                        }
                    }
                    //sync number of thread checks so we dont close too many
                    //check current threads for pacs and if there aren't too many running, pull another study from pacs
                    //make syncing pacs specific instead of over all pacs
                    //...
                } else {
                    continueThread.set(false);
                }
            }
        } catch (Throwable exception) {
            log.error("Error executing a PACS request from the queue.", exception);
        } finally {
            synchronized (THREAD_COUNT_LOCK) {
                Integer currThreads = currentThreadsPerPacs.get(_pacsId);
                currentThreadsPerPacs.put(_pacsId, currThreads - 1);
            }
        }
    }

    private void dequeueRequest(final double utilizationPercent, final List<QueuedPacsRequest> requests) {
        final QueuedPacsRequest queued = requests.get(0);
        queued.setStatus(PacsRequest.PROCESSING_STATUS_TEXT);
        _queuedPacsRequestService.update(queued);

        final StopWatch stopWatch        = StopWatch.create();
        final String    login            = _userProvider.getLogin();
        final String    username         = queued.getUsername();
        final String    projectId        = queued.getXnatProject();
        final String    studyInstanceUid = queued.getStudyInstanceUid();
        final String    seriesIds        = queued.getSeriesIds();
        final String    currAnonScript   = queued.getRemappingScript();
        final String    path             = "/studies/" + studyInstanceUid;
        log.debug("User {} is setting {} script for project {}", username, DicomEdit.ToolName, studyInstanceUid);

        final ExecutedPacsRequest request = new ExecutedPacsRequest();
        request.setPacsId(_pacsId);
        request.setUsername(username);
        request.setXnatProject(projectId);
        request.setStudyInstanceUid(studyInstanceUid);
        request.setSeriesIds(seriesIds);
        request.setDestinationAeTitle(queued.getDestinationAeTitle());
        request.setStatus(PacsRequest.ISSUED_STATUS_TEXT);
        request.setExecutedTime(new Date());
        request.setQueuedTime(queued.getQueuedTime());
        request.setStudyDate(queued.getStudyDate());
        request.setStudyId(queued.getStudyId());
        request.setAccessionNumber(queued.getAccessionNumber());
        request.setPacsId(queued.getPacsId());
        request.setPatientName(queued.getPatientName());

        _executedPacsRequestService.create(request);

        try {
            if (StringUtils.isNotBlank(currAnonScript)) {
                if (studyInstanceUid == null) {
                    _configService.replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript);
                } else {
                    _studyRoutingService.close(studyInstanceUid);
                    _configService.replaceConfig(login, "", DicomEdit.ToolName, path, currAnonScript, Scope.Site, studyInstanceUid);
                    _configService.enable(login, "", DicomEdit.ToolName, path, Scope.Site, studyInstanceUid);
                }
            }

            stopWatch.start();
            _pacsService.importFromPacsRequest(request);
            stopWatch.stop();

            queued.setStatus(PacsRequest.ISSUED_STATUS_TEXT);
            _queuedPacsRequestService.update(queued);
        } catch (Exception e) {
            queued.setStatus(PacsRequest.FAILED_STATUS_TEXT);
            _queuedPacsRequestService.update(queued);

            request.setStatus(PacsRequest.FAILED_STATUS_TEXT);
            _executedPacsRequestService.update(request);
            log.error("Error executing PACS import request.", e);
        } finally {
            final long queuedId = queued.getId();
            try {
                _queuedPacsRequestService.delete(queuedId);
            } catch (Exception e) {
                log.error("Error removing PACS import request {} from queue.", queuedId, e);
            }
        }

        final PacsServiceResourceContext context = new PacsServiceResourceContext();
        context.put("prearchive", _prearchiveUrl);
        context.put("studyId", studyInstanceUid);
        context.put("seriesIds", Arrays.asList(seriesIds.split("\\s*,\\s*")));

        try {
            if (StringUtils.isBlank(projectId)) {
                log.debug("Completed DICOM request for study {} with no project assignment.", studyInstanceUid);
            } else {
                log.debug("Completed DICOM request for study {} assigned to project {}", studyInstanceUid, projectId);
            }
            if (_dqrPreferences.getNotifyAdminOnImport()) {
                final String adminEmail = _siteConfigPreferences.getAdminEmail();
                context.put("adminEmail", adminEmail);
                context.put("pacs", _pacsEntityService.retrieve(_pacsId));

                final String body = AdminUtils.populateVmTemplate(context, EMAIL_TEMPLATE);
                _mailService.sendHtmlMessage(adminEmail, adminEmail, _emailSubject, body);
            }

            final PersistentWorkflowI workflow = PersistentWorkflowUtils.buildOpenWorkflow(Users.getUser(username), XnatMrsessiondata.SCHEMA_ELEMENT_NAME, studyInstanceUid, projectId, EventUtils.newEventInstance(EventUtils.CATEGORY.DATA, EventUtils.TYPE.PROCESS, "IMPORT_FROM_PACS_REQUEST", null, "Series: " + seriesIds));
            assert workflow != null;
            PersistentWorkflowUtils.complete(workflow, workflow.buildEvent());
            TimeUnit.MICROSECONDS.sleep((long) ((((double) 100 / utilizationPercent) - 1) * stopWatch.getTime() * 1000));
        } catch (Exception exception) {
            log.warn("User " + username + " requested one or more DICOM series, but an error occurred sending the notification email.", exception);
        }
    }

    private static final Object THREAD_COUNT_LOCK = new Object();
    private static final Object QUEUE_LOCK        = new Object();
    private static final String EMAIL_TEMPLATE    = "/screens/dqr/email/SeriesRequested.vm";

    private final long                          _pacsId;
    private final PacsService                   _pacsService;
    private final PacsEntityService             _pacsEntityService;
    private final PacsAvailabilityEntityService _pacsAvailabilityEntityService;
    private final QueuedPacsRequestService      _queuedPacsRequestService;
    private final ExecutedPacsRequestService    _executedPacsRequestService;
    private final StudyRoutingService           _studyRoutingService;
    private final ConfigService                 _configService;
    private final SiteConfigPreferences         _siteConfigPreferences;
    private final DqrPreferences                _dqrPreferences;
    private final MailService                   _mailService;
    private final XnatUserProvider              _userProvider;
    private final String                        _emailSubject;
    private final String                        _prearchiveUrl;
}
