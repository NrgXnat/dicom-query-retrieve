package org.nrg.dqr.events.listeners.methods;

public class PacsAvailabilityCheckerHandlerMethod{

}
//import com.google.common.collect.ImmutableList;
//import groovy.util.logging.Slf4j;
//import jdk.nashorn.internal.objects.annotations.Getter;
//import jdk.nashorn.internal.objects.annotations.Setter;
//import org.nrg.dqr.events.PacsRequestDequeuer;
//import org.nrg.xdat.preferences.SiteConfigPreferences;
//import org.nrg.xnat.event.listeners.methods.AbstractScheduledXnatPreferenceHandlerMethod;
//import org.springframework.beans.factory.annotation.Autowired;
//import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;
//import org.springframework.scheduling.support.PeriodicTrigger;
//import org.springframework.stereotype.Component;
//
//import static lombok.AccessLevel.PRIVATE;
//import static lombok.AccessLevel.PROTECTED;
//
//import java.util.*;
//import java.util.concurrent.ScheduledFuture;
//
//import static org.nrg.framework.orm.DatabaseHelper.convertPGIntervalToIntSeconds;
//
///**
// * Created by mike on 1/23/18.
// */
//@Component
//@Slf4j
//@Getter(PROTECTED)
//@Setter(AccessLevel.PRIVATE)
//@Accessors(prefix = "_")
//public class PacsAvailabilityCheckerHandlerMethod extends AbstractScheduledXnatPreferenceHandlerMethod {
//    @Autowired
//    public PacsAvailabilityCheckerHandlerMethod(final SiteConfigPreferences preferences, final ThreadPoolTaskScheduler scheduler) {
//        _preferences = preferences;
//        _scheduler = scheduler;
//    }
//
//    @Override
//    public List<String> getHandledPreferences() {
//        return PREFERENCES;
//    }
//
//    @Override
//    public void handlePreferences(final Map<String, String> values) {
////        if (!Collections.disjoint(PREFERENCES, values.keySet())) {
////            updatePacsAvailabilityChecker();
////        }
//    }
//
//    @Override
//    public void handlePreference(final String preference, final String value) {
////        if (PREFERENCES.contains(preference)) {
////            updatePacsAvailabilityChecker();
////        }
//    }
//
//    private void updatePacsAvailabilityChecker() {
//        _scheduler.getScheduledThreadPoolExecutor().setRemoveOnCancelPolicy(true);
//        for (ScheduledFuture temp : _scheduledPacsRequestDequeuer) {
//            temp.cancel(false);
//        }
//        _scheduledPacsRequestDequeuer.clear();
//        try {
//            _scheduledPacsRequestDequeuer.add(_scheduler.schedule(new PacsRequestDequeuer(_preferences), new PeriodicTrigger(1000 * SiteConfigPreferences.convertPGIntervalToSeconds(_preferences.get("pacsAvailabilityCheckFrequency").toString()))));
//        }catch(Exception e){
//            //Ignore
//        }
//    }
//
//    /**
//     * Updates the value for the specified preference according to the preference type.
//     *
//     * @param preference The preference to set.
//     * @param value      The value to set.
//     */
//    protected void handlePreferenceImpl(final String preference, final String value) {
//        log.debug("Found preference {} that this handler can handle, setting value to {}", preference, value);
////        switch (preference) {
////            case AVAILABILITY_CHECK_FREQUENCY:
////                (updatePacsAvailabilityChecker(value));
////                break;
////        }
//    }
//
//    private static final String AVAILABILITY_CHECK_FREQUENCY   = "pacsAvailabilityCheckFrequency";
//    private final ArrayList<ScheduledFuture> _scheduledPacsRequestDequeuer = new ArrayList<>();
//
//    private final SiteConfigPreferences   _preferences;
//    private final ThreadPoolTaskScheduler _scheduler;
//}
