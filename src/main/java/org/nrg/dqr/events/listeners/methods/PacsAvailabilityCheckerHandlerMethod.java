package org.nrg.dqr.events.listeners.methods;

import lombok.Getter;
import lombok.Setter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;
import org.nrg.dqr.events.PacsRequestDequeuer;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xnat.security.ResetFailedLogins;
import org.nrg.xnat.task.AbstractXnatRunnable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.Trigger;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;
import org.nrg.xnat.event.listeners.methods.AbstractScheduledXnatPreferenceHandlerMethod;
import java.util.*;
import java.util.concurrent.ScheduledFuture;

import static lombok.AccessLevel.PRIVATE;
import static lombok.AccessLevel.PROTECTED;
import static org.nrg.framework.orm.DatabaseHelper.convertPGIntervalToIntSeconds;

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
    public PacsAvailabilityCheckerHandlerMethod(final DqrPreferences preferences, final ThreadPoolTaskScheduler scheduler) {
        super(scheduler, AVAILABILITY_CHECK_FREQUENCY);
        Object checkFrequencyObject = preferences.get(AVAILABILITY_CHECK_FREQUENCY);
        if(checkFrequencyObject!=null && StringUtils.isNotBlank(checkFrequencyObject.toString())){
            setPacsAvailabilityCheckFrequency(preferences.get(AVAILABILITY_CHECK_FREQUENCY).toString());
        }
        else{
            setPacsAvailabilityCheckFrequency(DEFAULT_CHECK_FREQUENCY);
        }

    }

    @Override
    protected AbstractXnatRunnable getTask() {
        return new PacsRequestDequeuer();
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
        switch (preference) {
            case AVAILABILITY_CHECK_FREQUENCY:
                setPacsAvailabilityCheckFrequency(value);
                break;
        }
    }

    private static final String DEFAULT_CHECK_FREQUENCY = "10 minutes";
    private static final String AVAILABILITY_CHECK_FREQUENCY   = "pacsAvailabilityCheckFrequency";
    private final ArrayList<ScheduledFuture> _scheduledPacsRequestDequeuer = new ArrayList<>();

    private String _pacsAvailabilityCheckFrequency;
}
