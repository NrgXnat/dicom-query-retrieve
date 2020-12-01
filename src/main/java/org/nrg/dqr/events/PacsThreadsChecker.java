package org.nrg.dqr.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.services.ConfigService;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.*;
import org.nrg.framework.exceptions.NrgServiceRuntimeException;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.om.XdatUser;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.security.user.exceptions.UserInitException;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnat.task.AbstractXnatRunnable;

import java.time.LocalDateTime;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
public class PacsThreadsChecker extends AbstractXnatRunnable {
    public PacsThreadsChecker(final PacsService pacsService, final PacsEntityService pacsEntityService, final PacsAvailabilityEntityService pacsAvailabilityEntityService, final QueuedPacsRequestService queuedPacsRequestService, final ExecutedPacsRequestService executedPacsRequestService, final StudyRoutingService studyRoutingService, final SiteConfigPreferences siteConfigPreferences, final DqrPreferences dqrPreferences, final ConfigService configService, final MailService mailService, final XnatUserProvider userProvider) {
        log.trace("Initializing the PACS threads checker job");
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
    }

    @Override
    public void runTask() {
        log.debug("Executing PACS threads checker function");
        final Map<Long, Integer> currentThreadsPerPacs = PacsDequeueThread.currentThreadsPerPacs;
        for (final Pacs pacs : _pacsEntityService.findAllQueryable()) {
            final long pacsId = pacs.getId();
            try {
                if (_pacsService.canConnect(_userProvider.get(), _pacsEntityService.retrieve(pacsId))) {
                    _pacsAvailabilityEntityService.findAllByPacsIdAndDayOfWeek(pacsId, LocalDateTime.now().getDayOfWeek())
                                                  .stream()
                                                  .filter(entity -> entity.isAvailableNow() && entity.getUtilizationPercent() > 0 && entity.getThreads() > 0)
                                                  .findAny().ifPresent(entity -> {
                        final AtomicInteger added             = new AtomicInteger();
                        final long          newThreadsAllowed = entity.getThreads() - (currentThreadsPerPacs.get(pacsId) == null ? 0 : currentThreadsPerPacs.get(pacsId));
                        for (final QueuedPacsRequest request : _queuedPacsRequestService.getAllForPacsOrderedByPriorityAndDate(pacsId)) {
                            // TODO: Note that the first parameter of the PacsDequeueThread constructor is *always* the same as pacsId. Basically you can just check whether there are ANY requests for this PACS then kick off dequeue if so. There's no check that the dequeue operation is going to handle the current request.
                            new Thread(new PacsDequeueThread(request.getPacsId(), _pacsService, _pacsEntityService, _pacsAvailabilityEntityService, _queuedPacsRequestService, _executedPacsRequestService, _studyRoutingService, _siteConfigPreferences, _dqrPreferences, _configService, _mailService, _userProvider)).start();
                            if (added.incrementAndGet() >= newThreadsAllowed) {
                                break;
                            }
                        }
                    });
                }
            } catch (Throwable e) {
                if (e instanceof NrgServiceRuntimeException && e.getCause() instanceof UserInitException && StringUtils.contains(e.getCause().getMessage(), XdatUser.SCHEMA_ELEMENT_NAME) && USER_INIT_FAIL_COUNT.incrementAndGet() < 5) {
                    log.debug("Got a user init exception, which probably means the system is still starting up. Specific message was: \"{}\"", e.getCause().getMessage());
                } else {
                    log.error("An error occurred while trying to get requests to dequeue for PACS {}.", pacsId, e);
                }
            }
        }
    }

    private static final AtomicInteger USER_INIT_FAIL_COUNT = new AtomicInteger();

    private final PacsService                   _pacsService;
    private final PacsEntityService             _pacsEntityService;
    private final PacsAvailabilityEntityService _pacsAvailabilityEntityService;
    private final QueuedPacsRequestService      _queuedPacsRequestService;
    private final ExecutedPacsRequestService    _executedPacsRequestService;
    private final StudyRoutingService           _studyRoutingService;
    private final SiteConfigPreferences         _siteConfigPreferences;
    private final DqrPreferences                _dqrPreferences;
    private final ConfigService                 _configService;
    private final MailService                   _mailService;
    private final XnatUserProvider              _userProvider;
}
