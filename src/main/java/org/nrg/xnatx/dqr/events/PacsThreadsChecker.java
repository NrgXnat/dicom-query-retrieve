/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.events.PacsThreadsChecker
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.events;

import lombok.extern.slf4j.Slf4j;
import org.nrg.config.services.ConfigService;
import org.nrg.framework.exceptions.NrgServiceError;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xft.security.UserI;
import org.nrg.xnat.task.AbstractXnatRunnable;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.*;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
public class PacsThreadsChecker extends AbstractXnatRunnable {
    public PacsThreadsChecker(final PacsThreads threads, final PacsService pacsService, final PacsEntityService pacsEntityService, final QueuedPacsRequestService queuedPacsRequestService, final ExecutedPacsRequestService executedPacsRequestService, final PacsAvailabilityService pacsAvailabilityService, final StudyRoutingService studyRoutingService, final DqrPreferences dqrPreferences, final SiteConfigPreferences siteConfigPreferences, final ConfigService configService, final MailService mailService, final XnatUserProvider primaryAdminUserProvider) {
        log.debug("Initializing the PACS threads checker job");
        _threads = threads;
        _pacsService = pacsService;
        _pacsEntityService = pacsEntityService;
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
            log.debug("Executing PACS threads checker function");
            final List<Pacs> pacsList = _pacsEntityService.findAllQueryable();
            if (pacsList != null) {
                for (final Pacs pacs : pacsList) {
                    try {
                        final long                       pacsId          = pacs.getId();
                        final Optional<PacsAvailability> getAvailability = _pacsAvailabilityService.findAvailableNow(pacsId);
                        if (!getAvailability.isPresent()) {
                            continue;
                        }
                        final PacsAvailability availability = getAvailability.get();
                        if (!_threads.hasAvailable(pacsId, availability.getThreads())) {
                            continue;
                        }
                        final List<QueuedPacsRequest> requests = _queuedPacsRequestService.getAllForPacsOrderedByPriorityAndDate(pacsId);
                        if (requests.isEmpty()) {
                            continue;
                        }
                        try {
                            final UserI admin = _primaryAdminUserProvider.get();
                            if (_pacsService.canConnect(admin, pacs)) {
                                final AtomicInteger added                     = new AtomicInteger();
                                final int           currentThreadsForThisPacs = _threads.get(pacsId);
                                final long          newThreadsAllowed         = availability.getThreads() - currentThreadsForThisPacs;
                                for (final QueuedPacsRequest request : requests) {
                                    new Thread(new PacsDequeueThread(request.getPacsId(), _threads, _pacsService, _pacsEntityService, _queuedPacsRequestService, _executedPacsRequestService, _pacsAvailabilityService, _studyRoutingService, _dqrPreferences, _siteConfigPreferences, _configService, _mailService, _primaryAdminUserProvider)).start();
                                    if (added.incrementAndGet() >= newThreadsAllowed) {
                                        break;
                                    }
                                }
                            }
                        } catch (NrgServiceRuntimeException e) {
                            if (e.getServiceError() == NrgServiceError.UserServiceError) {
                                log.info("Got a user service error trying to retrieve admin user, which usually means we're starting up.");
                            } else {
                                log.error("Got a service runtime exception", e);
                            }
                        }
                    } catch (Exception e) {
                        log.error("Error getting requests to dequeue for PACS {}.", pacs.getId(), e);
                    }
                }
            }
        } catch (Throwable exception) {
            log.error("Error executing a PACS request from the queue.", exception);
        }
    }

    private final PacsThreads                   _threads;
    private final PacsService                   _pacsService;
    private final PacsEntityService             _pacsEntityService;
    private final QueuedPacsRequestService      _queuedPacsRequestService;
    private final ExecutedPacsRequestService _executedPacsRequestService;
    private final PacsAvailabilityService    _pacsAvailabilityService;
    private final StudyRoutingService        _studyRoutingService;
    private final DqrPreferences                _dqrPreferences;
    private final SiteConfigPreferences         _siteConfigPreferences;
    private final ConfigService                 _configService;
    private final MailService                   _mailService;
    private final XnatUserProvider              _primaryAdminUserProvider;
}
