package org.nrg.dqr.events.listeners.methods;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;
import static org.nrg.framework.orm.DatabaseHelper.convertPGIntervalToIntSeconds;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.dqr.events.PacsThreadsChecker;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.*;
import org.nrg.xnat.event.listeners.methods.AbstractScheduledXnatPreferenceHandlerMethod;
import org.nrg.xnat.task.AbstractXnatRunnable;
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
    public PacsAvailabilityCheckerHandlerMethod(final DqrPreferences preferences, final ThreadPoolTaskScheduler scheduler, final PacsEntityService pacsEntityService, final PacsService pacsService, final PacsAvailabilityEntityService pacsAvailabilityEntityService, final QueuedPacsRequestService queueService, final ExecutedPacsRequestService executedService) {
        super(scheduler, AVAILABILITY_CHECK_FREQUENCY);

        _pacsEntityService = pacsEntityService;
        _pacsService = pacsService;
        _pacsAvailabilityEntityService = pacsAvailabilityEntityService;
        _queueService = queueService;
        _executedService = executedService;

        setPacsAvailabilityCheckFrequency(StringUtils.defaultIfBlank(preferences.getPacsAvailabilityCheckFrequency(), DEFAULT_CHECK_FREQUENCY));
    }

    @Override
    protected AbstractXnatRunnable getTask() {
        return new PacsThreadsChecker(_pacsEntityService, _pacsService, _pacsAvailabilityEntityService, _queueService, _executedService);
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
        if (AVAILABILITY_CHECK_FREQUENCY.equals(preference)) {
            setPacsAvailabilityCheckFrequency(value);
        }
    }

    private static final String DEFAULT_CHECK_FREQUENCY      = "1 minute";
    private static final String AVAILABILITY_CHECK_FREQUENCY = "pacsAvailabilityCheckFrequency";

    private final PacsEntityService             _pacsEntityService;
    private final PacsService                   _pacsService;
    private final PacsAvailabilityEntityService _pacsAvailabilityEntityService;
    private final QueuedPacsRequestService      _queueService;
    private final ExecutedPacsRequestService    _executedService;

    private String _pacsAvailabilityCheckFrequency;
}
