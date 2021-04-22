package org.nrg.xnatx.dqr.events.listeners.methods;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;
import static org.nrg.framework.orm.DatabaseHelper.convertPGIntervalToIntSeconds;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.config.services.ConfigService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnat.event.listeners.methods.AbstractScheduledXnatPreferenceHandlerMethod;
import org.nrg.xnat.task.AbstractXnatRunnable;
import org.nrg.xnatx.dqr.events.PacsThreads;
import org.nrg.xnatx.dqr.events.PacsThreadsChecker;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

/**
 * Created by mike on 1/23/18.
 */
@Component
@Slf4j
@Getter(PROTECTED)
@Setter(PRIVATE)
@Accessors(prefix = "_")
public class PacsAvailabilityCheckerHandlerMethod extends AbstractScheduledXnatPreferenceHandlerMethod {
    @Autowired
    public PacsAvailabilityCheckerHandlerMethod(final DqrPreferences preferences, final ThreadPoolTaskScheduler scheduler, final PacsEntityService pacsEntityService, final PacsService pacsService, final PacsAvailabilityEntityService pacsAvailabilityEntityService, final QueuedPacsRequestService queuedPacsRequestService, final ExecutedPacsRequestService executedPacsRequestService, final StudyRoutingService studyRoutingService, final DqrPreferences dqrPreferences, final PacsThreads threads, final SiteConfigPreferences siteConfigPreferences, final ConfigService configService, final MailService mailService, final XnatUserProvider primaryAdminUserProvider) {
        super(scheduler, AVAILABILITY_CHECK_FREQUENCY);

        _pacsEntityService = pacsEntityService;
        _pacsService = pacsService;
        _pacsAvailabilityEntityService = pacsAvailabilityEntityService;
        _queuedPacsRequestService = queuedPacsRequestService;
        _executedPacsRequestService = executedPacsRequestService;
        _studyRoutingService = studyRoutingService;
        _dqrPreferences = dqrPreferences;
        _threads = threads;
        _siteConfigPreferences = siteConfigPreferences;
        _configService = configService;
        _mailService = mailService;
        _primaryAdminUserProvider = primaryAdminUserProvider;

        setPacsAvailabilityCheckFrequency(StringUtils.defaultIfBlank(preferences.getPacsAvailabilityCheckFrequency(), DEFAULT_CHECK_FREQUENCY));
    }

    @Override
    protected AbstractXnatRunnable getTask() {
        return new PacsThreadsChecker(_threads, _pacsService, _pacsEntityService, _queuedPacsRequestService, _executedPacsRequestService, _pacsAvailabilityEntityService, _studyRoutingService, _dqrPreferences, _siteConfigPreferences, _configService, _mailService, _primaryAdminUserProvider);
    }

    @Override
    protected Trigger getTrigger() {
        return new PeriodicTrigger(1000L * convertPGIntervalToIntSeconds(getPacsAvailabilityCheckFrequency()));
    }

    /**
     * Updates the value for the specified preference according to the preference type.
     *
     * @param preference The preference to set.
     * @param value      The value to set.
     */
    @Override
    protected void handlePreferenceImpl(final String preference, final String value) {
        log.debug("Found preference {} that this handler can handle, setting value to {}", preference, value);
        if (AVAILABILITY_CHECK_FREQUENCY.equals(preference)) {
            setPacsAvailabilityCheckFrequency(value);
        }
    }

    private static final String DEFAULT_CHECK_FREQUENCY      = "1 minute";
    private static final String AVAILABILITY_CHECK_FREQUENCY = "pacsAvailabilityCheckFrequency";

    private final PacsEntityService             _pacsEntityService;
    private final PacsService                   _pacsService;
    private final PacsAvailabilityEntityService _pacsAvailabilityEntityService;
    private final QueuedPacsRequestService      _queuedPacsRequestService;
    private final ExecutedPacsRequestService    _executedPacsRequestService;
    private final StudyRoutingService           _studyRoutingService;
    private final DqrPreferences                _dqrPreferences;
    private final PacsThreads                   _threads;
    private final SiteConfigPreferences         _siteConfigPreferences;
    private final ConfigService                 _configService;
    private final MailService                   _mailService;
    private final XnatUserProvider              _primaryAdminUserProvider;

    private String _pacsAvailabilityCheckFrequency;
}
