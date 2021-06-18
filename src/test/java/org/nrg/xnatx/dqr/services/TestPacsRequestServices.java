package org.nrg.xnatx.dqr.services;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.RandomStringUtils;
import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.time.StopWatch;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nrg.xnatx.dqr.domain.TestPacsRequestServicesConfig;
import org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.Pacs;
import org.nrg.xnatx.dqr.domain.entities.PaginatedPacsRequest;
import org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.xnatx.dqr.utils.DqrDateRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitJupiterConfig;

import java.time.Duration;
import java.time.Instant;
import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import static org.assertj.core.api.AssertionsForInterfaceTypes.assertThat;
import static org.nrg.xnatx.dqr.domain.entities.PacsRequest.*;

@ExtendWith(SpringExtension.class)
@SpringJUnitJupiterConfig(TestPacsAvailabilityService.class)
@ContextConfiguration(classes = TestPacsRequestServicesConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
public class TestPacsRequestServices {
    private static final int NUM_QUEUED_REQUESTS   = 5000;
    private static final int NUM_EXECUTED_REQUESTS = 5000;

    private final PacsService                _pacsService;
    private final QueuedPacsRequestService   _queuedPacsRequestService;
    private final ExecutedPacsRequestService _executedPacsRequestService;

    @Autowired
    public TestPacsRequestServices(final PacsService pacsService, final QueuedPacsRequestService queuedPacsRequestService, final ExecutedPacsRequestService executedPacsRequestService) {
        _pacsService                = pacsService;
        _queuedPacsRequestService   = queuedPacsRequestService;
        _executedPacsRequestService = executedPacsRequestService;
    }

    @Test
    public void testPaging() {
        final List<Pacs>                pacs             = getPacs();
        final List<QueuedPacsRequest>   queuedRequests   = IntStream.range(0, NUM_QUEUED_REQUESTS).boxed().map(index -> _queuedPacsRequestService.create(getRandomQueuedPacsRequest(pacs))).collect(Collectors.toList());
        final List<ExecutedPacsRequest> executedRequests = IntStream.range(0, NUM_EXECUTED_REQUESTS).boxed().map(index -> _executedPacsRequestService.create(getRandomExecutedPacsRequest(pacs))).collect(Collectors.toList());
        assertThat(queuedRequests).isNotNull().isNotEmpty().hasSize(NUM_QUEUED_REQUESTS);
        assertThat(executedRequests).isNotNull().isNotEmpty().hasSize(NUM_EXECUTED_REQUESTS);

        final Map<String, Long> expectedQueuedRequestCounts   = queuedRequests.stream().collect(Collectors.groupingBy(QueuedPacsRequest::getUsername, Collectors.counting()));
        final Map<String, Long> expectedExecutedRequestCounts = executedRequests.stream().collect(Collectors.groupingBy(ExecutedPacsRequest::getUsername, Collectors.counting()));
        Assertions.assertThat(expectedQueuedRequestCounts).isNotNull().isNotEmpty().hasSize(USERNAMES.size()).containsOnlyKeys(USERNAMES);
        Assertions.assertThat(expectedQueuedRequestCounts.values().stream().reduce(0L, Long::sum)).isEqualTo(NUM_QUEUED_REQUESTS);
        Assertions.assertThat(expectedExecutedRequestCounts).isNotNull().isNotEmpty().hasSize(USERNAMES.size()).containsOnlyKeys(USERNAMES);
        Assertions.assertThat(expectedExecutedRequestCounts.values().stream().reduce(0L, Long::sum)).isEqualTo(NUM_EXECUTED_REQUESTS);

        final StopWatch         stopWatch                 = StopWatch.createStarted();
        final Map<String, Long> actualQueuedRequestCounts = USERNAMES.stream().collect(Collectors.toMap(Function.identity(), _queuedPacsRequestService::getAllForUserCount));
        stopWatch.stop();
        log.info("Retrieved counts of queued requests by username in {} milliseconds", stopWatch.getTime());
        stopWatch.reset();

        Assertions.assertThat(actualQueuedRequestCounts).isNotNull().isNotEmpty().containsExactlyInAnyOrderEntriesOf(expectedQueuedRequestCounts);
        Assertions.assertThat(actualQueuedRequestCounts.values().stream().reduce(0L, Long::sum)).isEqualTo(NUM_QUEUED_REQUESTS);

        stopWatch.start();
        final Map<String, Long> actualExecutedRequestCounts = USERNAMES.stream().collect(Collectors.toMap(Function.identity(), _executedPacsRequestService::getAllForUserCount));
        stopWatch.stop();
        log.info("Retrieved counts of executed requests by username in {} milliseconds", stopWatch.getTime());
        stopWatch.reset();

        Assertions.assertThat(actualExecutedRequestCounts).isNotNull().isNotEmpty().containsExactlyInAnyOrderEntriesOf(expectedExecutedRequestCounts);
        Assertions.assertThat(actualExecutedRequestCounts.values().stream().reduce(0L, Long::sum)).isEqualTo(NUM_EXECUTED_REQUESTS);

        stopWatch.start();
        final List<QueuedPacsRequest> queuedRequestsOrderedByDate = _queuedPacsRequestService.getAllOrderedByDate();
        stopWatch.stop();
        log.info("Retrieved all queued requests ordered by date in {} milliseconds", stopWatch.getTime());
        stopWatch.reset();

        Assertions.assertThat(queuedRequestsOrderedByDate).isNotNull().isNotEmpty().hasSize(NUM_QUEUED_REQUESTS);

        final PaginatedPacsRequest paginatedQueuedPacsRequest = PaginatedPacsRequest.builder().pageNumber(1).pageSize(200).build();

        stopWatch.start();
        final List<QueuedPacsRequest> first200QueuedRequestsOrderedByDate = _queuedPacsRequestService.getAllOrderedByDate(paginatedQueuedPacsRequest);
        stopWatch.stop();
        log.info("Retrieved page of queued requests ordered by date in {} milliseconds", stopWatch.getTime());
        stopWatch.reset();

        Assertions.assertThat(first200QueuedRequestsOrderedByDate).isNotNull().isNotEmpty().hasSize(200).containsAll(queuedRequestsOrderedByDate.subList(0, 200));

        stopWatch.start();
        final List<ExecutedPacsRequest> executedRequestsOrderedByDate = _executedPacsRequestService.getAllOrderedByDate();
        stopWatch.stop();
        log.info("Retrieved all executed requests ordered by date in {} milliseconds", stopWatch.getTime());
        stopWatch.reset();

        Assertions.assertThat(executedRequestsOrderedByDate).isNotNull().isNotEmpty().hasSize(NUM_EXECUTED_REQUESTS);

        final PaginatedPacsRequest paginatedExecutedPacsRequest = PaginatedPacsRequest.builder().pageNumber(1).pageSize(200).build();

        stopWatch.start();
        final List<ExecutedPacsRequest> first200ExecutedRequestsOrderedByDate = _executedPacsRequestService.getAllOrderedByDate(paginatedExecutedPacsRequest);
        stopWatch.stop();
        log.info("Retrieved page of executed requests ordered by date in {} milliseconds", stopWatch.getTime());
        stopWatch.reset();

        Assertions.assertThat(first200ExecutedRequestsOrderedByDate).isNotNull().isNotEmpty().hasSize(200).containsAll(executedRequestsOrderedByDate.subList(0, 200));
    }

    private List<Pacs> getPacs() {
        return PACS.stream().map(this::getPacs).collect(Collectors.toList());
    }

    private Pacs getPacs(final String name) {
        final boolean isDefault = StringUtils.equals("lab", name);
        return _pacsService.create(Pacs.builder().label(StringUtils.capitalize(name)).host("10.1.1.1").aeTitle(StringUtils.upperCase(name)).queryRetrievePort(4242).ormStrategySpringBeanId("dicomOrmStrategy").defaultQueryRetrievePacs(isDefault).defaultStoragePacs(isDefault).queryable(true).storable(true).supportsExtendedNegotiations(true).build());
    }

    private QueuedPacsRequest getRandomQueuedPacsRequest(final List<Pacs> pacs) {
        final QueuedPacsRequest.QueuedPacsRequestBuilder builder = QueuedPacsRequest.builder()
                                                                                    .pacsId(getRandom(pacs).getId())
                                                                                    .username(getRandom(USERNAMES))
                                                                                    .xnatProject(getRandom(PROJECTS))
                                                                                    .destinationAeTitle(getRandom(AE_TITLES))
                                                                                    .status(getRandom(STATUSES))
                                                                                    .queuedTime(getRandomDate())
                                                                                    .seriesIds(getRandomStrings())
                                                                                    .priority(RANDOM.nextBoolean() ? HIGH_PRIORITY : STANDARD_PRIORITY);
        if (RANDOM.nextBoolean()) {
            builder.studyInstanceUid(RandomStringUtils.randomAlphanumeric(20));
        }
        if (RANDOM.nextBoolean()) {
            builder.studyId(RandomStringUtils.randomAlphanumeric(20));
        }
        if (RANDOM.nextBoolean()) {
            builder.studyDate(RegExUtils.removeAll(DqrDateRange.formatDate(getRandomDate()), "-"));
        }
        if (RANDOM.nextBoolean()) {
            builder.accessionNumber(String.format("%09d", RANDOM.nextInt(999900000) + 100000));
        }
        if (RANDOM.nextBoolean()) {
            builder.patientId(String.format("%09d", RANDOM.nextInt(999900000) + 100000));
        }
        if (RANDOM.nextBoolean()) {
            builder.patientName(getRandomName(20) + "^" + getRandomName(12));
        }
        return builder.build();
    }

    private ExecutedPacsRequest getRandomExecutedPacsRequest(final List<Pacs> pacs) {
        final Date queuedDate = getRandomDate();
        final ExecutedPacsRequest.ExecutedPacsRequestBuilder builder = ExecutedPacsRequest.builder()
                                                                                          .pacsId(getRandom(pacs).getId())
                                                                                          .username(getRandom(USERNAMES))
                                                                                          .xnatProject(getRandom(PROJECTS))
                                                                                          .destinationAeTitle(getRandom(AE_TITLES))
                                                                                          .status(getRandom(STATUSES))
                                                                                          .queuedTime(queuedDate)
                                                                                          .executedTime(getRandomDate(queuedDate))
                                                                                          .seriesIds(getRandomStrings())
                                                                                          .priority(RANDOM.nextBoolean() ? HIGH_PRIORITY : STANDARD_PRIORITY);
        if (RANDOM.nextBoolean()) {
            builder.studyInstanceUid(RandomStringUtils.randomAlphanumeric(20));
        }
        if (RANDOM.nextBoolean()) {
            builder.studyId(RandomStringUtils.randomAlphanumeric(20));
        }
        if (RANDOM.nextBoolean()) {
            builder.studyDate(RegExUtils.removeAll(DqrDateRange.formatDate(getRandomDate()), "-"));
        }
        if (RANDOM.nextBoolean()) {
            builder.accessionNumber(String.format("%09d", RANDOM.nextInt(999900000) + 100000));
        }
        if (RANDOM.nextBoolean()) {
            builder.patientId(String.format("%09d", RANDOM.nextInt(999900000) + 100000));
        }
        if (RANDOM.nextBoolean()) {
            builder.patientName(getRandomName(20) + "^" + getRandomName(12));
        }
        return builder.build();
    }

    private static <T> T getRandom(final List<T> list) {
        return list.get(RANDOM.nextInt(list.size()));
    }

    private static Date getRandomDate() {
        return Date.from(Instant.now().minus(Duration.ofDays(RANDOM.nextInt(365))));
    }

    private static Date getRandomDate(final Date date) {
        return Date.from(Instant.ofEpochMilli(date.getTime()).plus(Duration.ofDays(RANDOM.nextInt(4))));
    }

    private static List<String> getRandomStrings() {
        return IntStream.range(0, RANDOM.nextInt(10)).boxed().map(index -> RandomStringUtils.randomAlphanumeric(20)).collect(Collectors.toList());
    }

    private static String getRandomName(final int maxLength) {
        return StringUtils.capitalize(StringUtils.lowerCase(RandomStringUtils.randomAlphabetic(5, maxLength)));
    }

    private static final Random       RANDOM    = new Random();
    private static final List<String> PACS      = Arrays.asList("lab", "upstairs", "downstairs", "uptown", "downtown");
    private static final List<String> USERNAMES = Arrays.asList("abarrett", "abean", "asummers", "hmckinney", "hwiley", "jlynn", "jrangel", "jhiggins", "rburns", "rlittle", "vmurray", "ysantos");
    private static final List<String> PROJECTS  = Arrays.asList("A", "B", "C", "D", "E", "F");
    private static final List<String> AE_TITLES = Arrays.asList("T", "U", "V", "W", "X", "Y", "Z");
    private static final List<String> STATUSES  = Arrays.asList(QUEUED_STATUS_TEXT, PROCESSING_STATUS_TEXT, ISSUED_STATUS_TEXT, FAILED_STATUS_TEXT, RECEIVED_STATUS_TEXT);
}
