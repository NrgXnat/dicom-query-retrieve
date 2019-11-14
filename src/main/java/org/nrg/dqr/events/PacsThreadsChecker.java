package org.nrg.dqr.events;

import org.apache.commons.lang.StringUtils;
import org.nrg.dqr.domain.entities.*;
import org.nrg.dqr.preferences.DqrPreferences;
import org.nrg.dqr.services.*;
import org.nrg.framework.constants.Scope;
import org.nrg.xdat.XDAT;
import org.nrg.xdat.om.XnatMrsessiondata;
import org.nrg.xdat.security.XDATUser;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xdat.turbine.utils.AdminUtils;
import org.nrg.xdat.turbine.utils.TurbineUtils;
import org.nrg.xft.event.EventDetails;
import org.nrg.xft.event.EventUtils;
import org.nrg.xft.event.persist.PersistentWorkflowI;
import org.nrg.xft.event.persist.PersistentWorkflowUtils;
import org.nrg.xnat.helpers.editscript.DicomEdit;
import org.nrg.xnat.restlet.extensions.*;
import org.restlet.data.Status;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.nrg.xnat.task.*;

import java.util.*;

/**
 * Created by mike on 1/23/18.
 */
public class PacsThreadsChecker extends AbstractXnatRunnable {
    public PacsThreadsChecker(){
        if (_log.isDebugEnabled()) {
            _log.debug("Initializing the PACS threads checker job");
        }
    }

    @Override
    public void runTask() {
        try {
            if (_log.isDebugEnabled()) {
                _log.debug("Executing PACS threads checker function");
            }
            Map<Long,Integer> currentThreadsPerPacs = PacsDequeueThread.currentThreadsPerPacs;
            PacsEntityService pacsEntityService = XDAT.getContextService().getBean(PacsEntityService.class);
            PacsService pacsService = XDAT.getContextService().getBean(PacsService.class);
            PacsAvailabilityEntityService pacsAvailabilityEntityService = XDAT.getContextService().getBean(PacsAvailabilityEntityService.class);
            QueuedPacsRequestService queueService = XDAT.getContextService().getBean(QueuedPacsRequestService.class);
            ExecutedPacsRequestService executedService = XDAT.getContextService().getBean(ExecutedPacsRequestService.class);
            List<QueuedPacsRequest> requestsToDequeue = new ArrayList<>();

            List<Pacs> pacsList = pacsEntityService.findAllQueryable();
            if(pacsList!=null) {
                for (Pacs currPacs : pacsList) {
                    try {
                        Long pacsId = currPacs.getId();
                        Calendar currentCal = Calendar.getInstance();
                        int currentDayOfWeek = currentCal.get(Calendar.DAY_OF_WEEK);
                        List<PacsAvailability> availabilityList = pacsAvailabilityEntityService.findSettingsByPacsByDay(pacsId,currentDayOfWeek);
                        int utilizationPercent = 0;
                        int threads = 0;
                        for (PacsAvailability availability : availabilityList) {
                            String availabilityStartTimeString = availability.getAvailabilityStart();
                            String availabilityEndTimeString = availability.getAvailabilityEnd();
                            int availabilityDay = availability.getDayOfWeek();

                            //If hour is one digit, pad with a zero.
                            if (availabilityStartTimeString.charAt(1) == ':') {
                                availabilityStartTimeString = "0" + availabilityStartTimeString;
                            }
                            if (availabilityEndTimeString.charAt(1) == ':') {
                                availabilityEndTimeString = "0" + availabilityEndTimeString;
                            }

                            long currMillis = currentCal.getTimeInMillis();

                            long startMillis = 0L;
                            long endMillis = 0L;

                            if (StringUtils.isNotBlank(availabilityStartTimeString)) {
                                try {
                                    Calendar startCal = (Calendar) currentCal.clone();
                                    String[] startTime = StringUtils.split(availabilityStartTimeString, ":");
                                    startCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(startTime[0]));
                                    startCal.set(Calendar.MINUTE, Integer.parseInt(startTime[1]));
                                    startMillis = startCal.getTimeInMillis();
                                } catch (Exception e) {

                                }
                            }
                            if (StringUtils.isNotBlank(availabilityEndTimeString)) {
                                try {
                                    Calendar endCal = (Calendar) currentCal.clone();
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
                        if (utilizationPercent >0 && threads>0) {
                            List<QueuedPacsRequest> reqs = queueService.getAllForPacsOrderedByPriorityAndDate(pacsId);
                            boolean canConnect = pacsService.canConnect(AdminUtils.getAdminUser(), pacsEntityService.retrieve(pacsId));
                            if (canConnect) {
                                int added = 0;
                                int currentThreadsForThisPacs = currentThreadsPerPacs.get(pacsId)==null?0:currentThreadsPerPacs.get(pacsId);
                                long newThreadsAllowed = threads-currentThreadsForThisPacs;
                                for (QueuedPacsRequest req : reqs) {
                                    Thread thread = new Thread(new PacsDequeueThread(req.getPacsId(), pacsAvailabilityEntityService, queueService, pacsService, pacsEntityService, executedService));
                                    thread.start();
                                    added++;
                                    if (added >= newThreadsAllowed) {
                                        break;
                                    }
                                }
                            }

                        }
                    }
                    catch(Exception e){
                        _log.error("Error getting requests to dequeue for PACS "+currPacs.getId()+".",e);
                    }
                }
            }
        } catch (Throwable exception) {
            _log.error("Error executing a PACS request from the queue.", exception);
        }
    }

    private static final Logger _log = LoggerFactory.getLogger(PacsThreadsChecker.class);
}
