package org.nrg.dqr.events.listeners.methods;

import com.google.common.collect.ImmutableList;
import org.nrg.dqr.events.PacsRequestDequeuer;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xnat.event.listeners.methods.AbstractSiteConfigPreferenceHandlerMethod;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
import org.springframework.scheduling.support.PeriodicTrigger;
import org.springframework.stereotype.Component;

import java.util.*;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by mike on 1/23/18.
 */
@Component
public class PacsAvailabilityCheckerHandlerMethod extends AbstractSiteConfigPreferenceHandlerMethod {
    @Autowired
    public PacsAvailabilityCheckerHandlerMethod(final SiteConfigPreferences preferences, final ThreadPoolTaskScheduler scheduler) {
        _preferences = preferences;
        _scheduler = scheduler;
    }

    @Override
    public List<String> getHandledPreferences() {
        return PREFERENCES;
    }

    @Override
    public void handlePreferences(final Map<String, String> values) {
        if (!Collections.disjoint(PREFERENCES, values.keySet())) {
            updatePacsAvailabilityChecker();
        }
    }

    @Override
    public void handlePreference(final String preference, final String value) {
        if (PREFERENCES.contains(preference)) {
            updatePacsAvailabilityChecker();
        }
    }

    private void updatePacsAvailabilityChecker() {
        _scheduler.getScheduledThreadPoolExecutor().setRemoveOnCancelPolicy(true);
        for (ScheduledFuture temp : _scheduledPacsRequestDequeuer) {
            temp.cancel(false);
        }
        _scheduledPacsRequestDequeuer.clear();
        try {
            _scheduledPacsRequestDequeuer.add(_scheduler.schedule(new PacsRequestDequeuer(_preferences), new PeriodicTrigger(1000 * SiteConfigPreferences.convertPGIntervalToSeconds(_preferences.get("pacsAvailabilityCheckFrequency").toString()))));
        }catch(Exception e){
            //Ignore
        }
    }

    private static final List<String> PREFERENCES = ImmutableList.copyOf(Arrays.asList("pacsAvailabilityCheckFrequency"));

    private final ArrayList<ScheduledFuture> _scheduledPacsRequestDequeuer = new ArrayList<>();

    private final SiteConfigPreferences   _preferences;
    private final ThreadPoolTaskScheduler _scheduler;
}
