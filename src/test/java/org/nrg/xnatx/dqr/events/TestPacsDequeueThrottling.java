package org.nrg.xnatx.dqr.events;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.nrg.config.services.ConfigService;
import org.nrg.mail.services.MailService;
import org.nrg.xdat.preferences.SiteConfigPreferences;
import org.nrg.xdat.security.user.XnatUserProvider;
import org.nrg.xdat.services.StudyRoutingService;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.xnatx.dqr.preferences.DqrPreferences;
import org.nrg.xnatx.dqr.services.DicomQueryRetrieveService;
import org.nrg.xnatx.dqr.services.ExecutedPacsRequestService;
import org.nrg.xnatx.dqr.services.PacsAvailabilityService;
import org.nrg.xnatx.dqr.services.PacsService;
import org.nrg.xnatx.dqr.services.QueuedPacsRequestService;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

/**
 * The pause between PACS requests is derived from how long the previous request took, which was a
 * safe assumption when every request retrieved a single series. A whole-study retrieve runs long
 * enough that the pause has to be bounded, or one request stalls the queue for hours.
 */
@ExtendWith(MockitoExtension.class)
class TestPacsDequeueThrottling {

    private static final long ONE_MINUTE      = 60000L;
    private static final long THIRTY_MINUTES  = 1800000L;

    @Mock private PacsThreads                threads;
    @Mock private DicomQueryRetrieveService  dqrService;
    @Mock private PacsService                pacsService;
    @Mock private QueuedPacsRequestService   queuedPacsRequestService;
    @Mock private ExecutedPacsRequestService executedPacsRequestService;
    @Mock private PacsAvailabilityService    pacsAvailabilityService;
    @Mock private StudyRoutingService        studyRoutingService;
    @Mock private DqrPreferences             dqrPreferences;
    @Mock private SiteConfigPreferences      siteConfigPreferences;
    @Mock private ConfigService              configService;
    @Mock private MailService                mailService;
    @Mock private XnatUserProvider           primaryAdminUserProvider;
    @Mock private PacsAvailability           availability;

    private PacsDequeueThread thread;

    @BeforeEach
    void setUp() {
        thread = new PacsDequeueThread(1L, threads, dqrService, pacsService, queuedPacsRequestService,
                executedPacsRequestService, pacsAvailabilityService, studyRoutingService, dqrPreferences,
                siteConfigPreferences, configService, mailService, primaryAdminUserProvider);
    }

    @Test
    void aPauseUnderTheCapStaysProportionalToTheRequest() {
        // Half utilization means pausing for as long as the request itself took
        when(availability.getUtilizationPercent()).thenReturn(50);
        when(dqrPreferences.getDqrMaxThrottleSleepMs()).thenReturn((int) THIRTY_MINUTES);

        assertThat(thread.calculateSleepTimeMillisecondsFromAvailability(ONE_MINUTE, availability)).isEqualTo(ONE_MINUTE);
    }

    @Test
    void aLongRequestDoesNotProduceAnUnboundedPause() {
        // A thirty minute study retrieve at 10% utilization asks for a four and a half hour pause
        when(availability.getUtilizationPercent()).thenReturn(10);
        when(dqrPreferences.getDqrMaxThrottleSleepMs()).thenReturn((int) THIRTY_MINUTES);

        assertThat(thread.calculateSleepTimeMillisecondsFromAvailability(THIRTY_MINUTES, availability)).isEqualTo(THIRTY_MINUTES);
    }

    @Test
    void aCapOfZeroLeavesThePauseUnbounded() {
        // Sites that would rather hold utilization exactly can opt out of the bound
        when(availability.getUtilizationPercent()).thenReturn(10);
        when(dqrPreferences.getDqrMaxThrottleSleepMs()).thenReturn(0);

        assertThat(thread.calculateSleepTimeMillisecondsFromAvailability(THIRTY_MINUTES, availability)).isEqualTo(9 * THIRTY_MINUTES);
    }

    @Test
    void fullUtilizationStillMeansNoPauseAtAll() {
        when(availability.getUtilizationPercent()).thenReturn(100);
        when(dqrPreferences.getDqrMaxThrottleSleepMs()).thenReturn((int) THIRTY_MINUTES);

        assertThat(thread.calculateSleepTimeMillisecondsFromAvailability(THIRTY_MINUTES, availability)).isZero();
    }
}
