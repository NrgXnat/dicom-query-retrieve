/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.events.listeners.methods.PacsAvailabilityCheckerHandlerMethod
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

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
import org.nrg.xnatx.dqr.services.ExecutedPacsRequestService;
import org.nrg.xnatx.dqr.services.PacsAvailabilityEntityService;
import org.nrg.xnatx.dqr.services.PacsEntityService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;
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
    public PacsAvailabilityCheckerHandlerMethod(final ThreadPoolTaskScheduler scheduler, final DqrPreferences dqrPreferences, final SiteConfigPreferences siteConfigPreferences, final ExecutedPacsRequestService executedPacsRequestService, final PacsAvailabilityEntityService pacsAvailabilityEntityService, final PacsEntityService pacsEntityService, final PacsService pacsService, final PacsThreads threads, final QueuedPacsRequestService queuedPacsRequestService, final StudyRoutingService studyRoutingService, final ConfigService configService, final MailService mailService, final XnatUserProvider primaryAdminUserProvider) {
        super(scheduler, AVAILABILITY_CHECK_FREQUENCY);
        final String checkFrequency = dqrPreferences.getPacsAvailabilityCheckFrequency();
        if (StringUtils.isNotBlank(checkFrequency)) {
            setPacsAvailabilityCheckFrequency(checkFrequency);
        } else {
            setPacsAvailabilityCheckFrequency(DEFAULT_CHECK_FREQUENCY);
        }

        _scheduler = scheduler;
        _dqrPreferences = dqrPreferences;
        _siteConfigPreferences = siteConfigPreferences;
        _executedPacsRequestService = executedPacsRequestService;
        _pacsAvailabilityEntityService = pacsAvailabilityEntityService;
        _pacsEntityService = pacsEntityService;
        _pacsService = pacsService;
        _threads = threads;
        _queuedPacsRequestService = queuedPacsRequestService;
        _studyRoutingService = studyRoutingService;
        _configService = configService;
        _mailService = mailService;
        _primaryAdminUserProvider = primaryAdminUserProvider;
    }

    @Override
    protected AbstractXnatRunnable getTask() {
        return new PacsThreadsChecker(_threads, _pacsService, _pacsEntityService, _queuedPacsRequestService, _executedPacsRequestService, _pacsAvailabilityEntityService, _studyRoutingService, _dqrPreferences, _siteConfigPreferences, _configService, _mailService, _primaryAdminUserProvider);
    }

    @Override
    protected Trigger getTrigger() {
        return new PeriodicTrigger(1000 * convertPGIntervalToIntSeconds(getPacsAvailabilityCheckFrequency()));
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
        setPacsAvailabilityCheckFrequency(value);
    }

    private static final String DEFAULT_CHECK_FREQUENCY      = "1 minute";
    private static final String AVAILABILITY_CHECK_FREQUENCY = "pacsAvailabilityCheckFrequency";

    private final ThreadPoolTaskScheduler       _scheduler;
    private final DqrPreferences                _dqrPreferences;
    private final SiteConfigPreferences         _siteConfigPreferences;
    private final ExecutedPacsRequestService    _executedPacsRequestService;
    private final PacsAvailabilityEntityService _pacsAvailabilityEntityService;
    private final PacsEntityService             _pacsEntityService;
    private final PacsService                   _pacsService;
    private final PacsThreads                   _threads;
    private final QueuedPacsRequestService      _queuedPacsRequestService;
    private final StudyRoutingService           _studyRoutingService;
    private final ConfigService                 _configService;
    private final MailService                   _mailService;
    private final XnatUserProvider              _primaryAdminUserProvider;

    private String _pacsAvailabilityCheckFrequency;
}
