package org.nrg.dqr.events;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang.StringUtils;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.dqr.domain.entities.PacsAvailability;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.dqr.services.*;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xnat.task.AbstractXnatRunnable;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Map;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
public class PacsThreadsChecker extends AbstractXnatRunnable {
    public PacsThreadsChecker(final PacsEntityService pacsEntityService, final PacsService pacsService, final PacsAvailabilityEntityService pacsAvailabilityEntityService, final QueuedPacsRequestService queueService, final ExecutedPacsRequestService executedService) {
        log.trace("Initializing the PACS threads checker job");
        _pacsEntityService = pacsEntityService;
        _pacsService = pacsService;
        _pacsAvailabilityEntityService = pacsAvailabilityEntityService;
        _queueService = queueService;
        _executedService = executedService;
    }

    @Override
    public void runTask() {
        log.debug("Executing PACS threads checker function");
        try {
            Map<Long, Integer>      currentThreadsPerPacs = PacsDequeueThread.currentThreadsPerPacs;
            List<QueuedPacsRequest> requestsToDequeue     = new ArrayList<>();

            List<Pacs> pacsList = _pacsEntityService.findAllQueryable();
            if (pacsList != null) {
                for (Pacs currPacs : pacsList) {
                    try {
                        Long                   pacsId             = currPacs.getId();
                        Calendar               currentCal         = Calendar.getInstance();
                        int                    currentDayOfWeek   = currentCal.get(Calendar.DAY_OF_WEEK);
                        List<PacsAvailability> availabilityList   = _pacsAvailabilityEntityService.findSettingsByPacsByDay(pacsId, currentDayOfWeek);
                        int                    utilizationPercent = 0;
                        int                    threads            = 0;
                        for (PacsAvailability availability : availabilityList) {
                            String availabilityStartTimeString = availability.getAvailabilityStart();
                            String availabilityEndTimeString   = availability.getAvailabilityEnd();
                            int    availabilityDay             = availability.getDayOfWeek();

                            //If hour is one digit, pad with a zero.
                            if (availabilityStartTimeString.charAt(1) == ':') {
                                availabilityStartTimeString = "0" + availabilityStartTimeString;
                            }
                            if (availabilityEndTimeString.charAt(1) == ':') {
                                availabilityEndTimeString = "0" + availabilityEndTimeString;
                            }

                            long currMillis = currentCal.getTimeInMillis();

                            long startMillis = 0L;
                            long endMillis   = 0L;

                            if (StringUtils.isNotBlank(availabilityStartTimeString)) {
                                try {
                                    Calendar startCal  = (Calendar) currentCal.clone();
                                    String[] startTime = StringUtils.split(availabilityStartTimeString, ":");
                                    startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTime[0]));
                                    startCal.set(Calendar.MINUTE, Integer.parseInt(startTime[1]));
                                    startMillis = startCal.getTimeInMillis();
                                } catch (Exception e) {

                                }
                            }
                            if (StringUtils.isNotBlank(availabilityEndTimeString)) {
                                try {
                                    Calendar endCal  = (Calendar) currentCal.clone();
                                    String[] endTime = StringUtils.split(availabilityEndTimeString, ":");
                                    endCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTime[0]));
                                    endCal.set(Calendar.MINUTE, Integer.parseInt(endTime[1]));
                                    endMillis = endCal.getTimeInMillis();
                                } catch (Exception e) {

                                }
                            }

                            boolean isAvailable = false;
                            if (endMillis < startMillis) {
                                //That means that the availability interval contains midnight.
                                if ((currMillis > startMillis && currentDayOfWeek == availabilityDay) || (currMillis < endMillis && currentDayOfWeek == (availabilityDay + 1))) {
                                    isAvailable = true;
                                }
                            } else {
                                if (currMillis > startMillis && currMillis < endMillis && currentDayOfWeek == availabilityDay) {
                                    isAvailable = true;
                                }
                            }

                            if (isAvailable) {
                                utilizationPercent = availability.getUtilizationPercent();
                                threads = availability.getThreads();
                                break;
                            }
                        }
                        if (utilizationPercent > 0 && threads > 0) {
                            List<QueuedPacsRequest> reqs       = _queueService.getAllForPacsOrderedByPriorityAndDate(pacsId);
                            boolean                 canConnect = _pacsService.canConnect(AdminUtils.getAdminUser(), _pacsEntityService.retrieve(pacsId));
                            if (canConnect) {
                                int  added                     = 0;
                                int  currentThreadsForThisPacs = currentThreadsPerPacs.get(pacsId) == null ? 0 : currentThreadsPerPacs.get(pacsId);
                                long newThreadsAllowed         = threads - currentThreadsForThisPacs;
                                for (QueuedPacsRequest req : reqs) {
                                    Thread thread = new Thread(new PacsDequeueThread(req.getPacsId(), _pacsAvailabilityEntityService, _queueService, _pacsService, _pacsEntityService, _executedService));
                                    thread.start();
                                    added++;
                                    if (added >= newThreadsAllowed) {
                                        break;
                                    }
                                }
                            }

                        }
                    } catch (Exception e) {
                        log.error("Error getting requests to dequeue for PACS " + currPacs.getId() + ".", e);
                    }
                }
            }
        } catch (Throwable exception) {
            log.error("Error executing a PACS request from the queue.", exception);
        }
    }

    private final PacsEntityService             _pacsEntityService;
    private final PacsService                   _pacsService;
    private final PacsAvailabilityEntityService _pacsAvailabilityEntityService;
    private final QueuedPacsRequestService      _queueService;
    private final ExecutedPacsRequestService    _executedService;
}
