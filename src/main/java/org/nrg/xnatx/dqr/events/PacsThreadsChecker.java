package org.nrg.xnatx.dqr.events;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.extern.slf4j.Slf4j;
import org.nrg.config.services.ConfigService;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.ExecutedPacsRequestService;
import org.nrg.xnatx.dqr.services.PacsAvailabilityEntityService;
import org.nrg.xnatx.dqr.services.PacsEntityService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnat.task.AbstractXnatRunnable;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
public class PacsThreadsChecker extends AbstractXnatRunnable {
    public PacsThreadsChecker(final PacsThreads threads, final PacsService pacsService, final PacsEntityService pacsEntityService, final QueuedPacsRequestService queuedPacsRequestService, final ExecutedPacsRequestService executedPacsRequestService, final PacsAvailabilityEntityService pacsAvailabilityEntityService, final StudyRoutingService studyRoutingService, final DqrPreferences dqrPreferences, final SiteConfigPreferences siteConfigPreferences, final ConfigService configService, final MailService mailService, final XnatUserProvider primaryAdminUserProvider) {
        log.debug("Initializing the PACS threads checker job");
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
            log.debug("Executing PACS threads checker function");
            final List<Pacs> pacsList = _pacsEntityService.findAllQueryable();
            if (pacsList != null) {
                for (final Pacs pacs : pacsList) {
                    try {
                        final long             pacsId       = pacs.getId();
                        final PacsAvailability availability = _pacsAvailabilityEntityService.findAvailableNow(pacsId);
                        if (availability != null && _threads.hasAvailable(pacsId, availability.getThreads())) {
                            final List<QueuedPacsRequest> requests = _queuedPacsRequestService.getAllForPacsOrderedByPriorityAndDate(pacsId);
                            if (_pacsService.canConnect(_primaryAdminUserProvider.get(), _pacsEntityService.retrieve(pacsId))) {
                                final AtomicInteger added                     = new AtomicInteger();
                                final int           currentThreadsForThisPacs = _threads.get(pacsId);
                                final long          newThreadsAllowed         = availability.getThreads() - currentThreadsForThisPacs;
                                for (final QueuedPacsRequest request : requests) {
                                    new Thread(new PacsDequeueThread(request.getPacsId(), _threads, _pacsService, _pacsEntityService, _queuedPacsRequestService, _executedPacsRequestService, _pacsAvailabilityEntityService, _studyRoutingService, _dqrPreferences, _siteConfigPreferences, _configService, _mailService, _primaryAdminUserProvider)).start();
                                    if (added.incrementAndGet() >= newThreadsAllowed) {
                                        break;
                                    }
                                }
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
    private final ExecutedPacsRequestService    _executedPacsRequestService;
    private final PacsAvailabilityEntityService _pacsAvailabilityEntityService;
    private final StudyRoutingService           _studyRoutingService;
    private final DqrPreferences                _dqrPreferences;
    private final SiteConfigPreferences         _siteConfigPreferences;
    private final ConfigService                 _configService;
    private final MailService                   _mailService;
    private final XnatUserProvider              _primaryAdminUserProvider;
}
