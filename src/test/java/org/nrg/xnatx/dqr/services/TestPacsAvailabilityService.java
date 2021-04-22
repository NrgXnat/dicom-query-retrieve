/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.PacsAvailabilityServiceTests
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2021, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nrg.xnatx.dqr.utils.DqrDateRange.HH_MM_FORMATTER;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.tuple.Pair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.nrg.xnatx.dqr.domain.DqrOrmTestConfiguration;
import org.nrg.xnatx.dqr.domain.TestPacsAvailabilityServiceConfig;
import org.nrg.xnatx.dqr.domain.entities.PacsAvailability;
import org.nrg.xnatx.dqr.utils.DqrDateRange;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.junit.jupiter.SpringJUnitJupiterConfig;

import java.time.*;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.LongStream;

@ExtendWith(SpringExtension.class)
@SpringJUnitJupiterConfig(TestPacsAvailabilityService.class)
@ContextConfiguration(classes = TestPacsAvailabilityServiceConfig.class)
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
@Slf4j
public class TestPacsAvailabilityService {
    @Autowired
    public TestPacsAvailabilityService(final PacsAvailabilityService service) {
        _service = service;
    }

    @Test
    public void sanityCheck() {
        log.info("This is a sanity check test. It should execute and exit successfully.");
        assertThat(_service).isNotNull();
    }

    @Test
    public void testSimplePacsAvailabilityTransaction() {
        final PacsAvailability availability1 = _service.create(PacsAvailability.builder().pacsId(1L).dayOfWeek(DayOfWeek.MONDAY).threads(4).utilizationPercent(100).availabilityStart("00:00").availabilityEnd("00:00").build());
        assertThat(availability1).isNotNull()
                                 .hasFieldOrPropertyWithValue("pacsId", 1L)
                                 .hasFieldOrPropertyWithValue("dayOfWeek", DayOfWeek.MONDAY)
                                 .hasFieldOrPropertyWithValue("threads", 4)
                                 .hasFieldOrPropertyWithValue("utilizationPercent", 100)
                                 .hasFieldOrPropertyWithValue("availabilityStart", "00:00")
                                 .hasFieldOrPropertyWithValue("availabilityEnd", "00:00");
        final PacsAvailability retrieved = _service.retrieve(availability1.getId());
        assertThat(retrieved).isNotNull()
                             .hasFieldOrPropertyWithValue("pacsId", 1L)
                             .hasFieldOrPropertyWithValue("dayOfWeek", DayOfWeek.MONDAY)
                             .hasFieldOrPropertyWithValue("threads", 4)
                             .hasFieldOrPropertyWithValue("utilizationPercent", 100)
                             .hasFieldOrPropertyWithValue("availabilityStart", "00:00")
                             .hasFieldOrPropertyWithValue("availabilityEnd", "00:00");
    }

    @Test
    public void testIsAvailableNow() {
        final PacsAvailability availability = PacsAvailability.builder().pacsId(1L).dayOfWeek(DayOfWeek.MONDAY).threads(4).utilizationPercent(100).availabilityStart("00:00").availabilityEnd("00:00").build();
        assertThat(availability).isNotNull().hasFieldOrPropertyWithValue("availableNow", true);
        final LocalTime now = LocalTime.now();
        final LocalTime start1 = now.minus(Duration.ofMinutes(1));
        final LocalTime end1 = now.plus(Duration.ofMinutes(5));
        availability.setAvailabilityStart(DqrDateRange.formatLocalTime(start1));
        availability.setAvailabilityEnd(end1.format(HH_MM_FORMATTER));
        assertThat(availability).isNotNull().hasFieldOrPropertyWithValue("availableNow", true);
        final LocalTime start2 = now.plus(Duration.ofMinutes(10));
        final LocalTime end2 = now.plus(Duration.ofMinutes(15));
        availability.setAvailabilityStart(start2.format(HH_MM_FORMATTER));
        availability.setAvailabilityEnd(end2.format(HH_MM_FORMATTER));
        assertThat(availability).isNotNull().hasFieldOrPropertyWithValue("availableNow", false);
    }

    @Test
    public void testGetDateRange() {
        final LocalTime time1 = LocalTime.parse("00:00");
        final LocalTime time2 = LocalTime.parse("12:00");

        final Pair<LocalDateTime, LocalDateTime> range1 = DqrDateRange.getDateRange(time1, time2);
        assertThat(range1.getLeft().toLocalTime()).isEqualTo(time1);
        assertThat(range1.getRight().toLocalTime()).isEqualTo(time2);

        final LocalTime time3 = LocalTime.parse("12:00");
        final LocalTime time4 = LocalTime.parse("00:00");

        final Pair<LocalDateTime, LocalDateTime> range2 = DqrDateRange.getDateRange(time3, time4);
        assertThat(range2.getLeft().toLocalTime()).isEqualTo(time3);
        assertThat(range2.getRight().toLocalTime()).isEqualTo(time4);
        assertThat(range2.getRight().toLocalDate()).isEqualTo(LocalDate.now().plusDays(1));
    }

    @Test
    public void testSearchAndAvailabilityFunctions() {
        final Map<Long, List<PacsAvailability>> availabilities = LongStream.range(1, 6).boxed().collect(Collectors.toMap(Function.identity(), pacsId -> IntStream.range(1, 8).mapToObj(DayOfWeek::of).map(dayOfWeek -> _service.create(PacsAvailability.builder().pacsId(pacsId).dayOfWeek(dayOfWeek).threads(4).utilizationPercent(100).availabilityStart("00:00").availabilityEnd("00:00").build())).collect(Collectors.toList())));
        assertThat(availabilities).isNotNull().isNotEmpty().hasSize(5).containsOnlyKeys(1L, 2L, 3L, 4L, 5L).allSatisfy((dayOfWeek, pacsAvailabilities) -> assertThat(pacsAvailabilities).hasSize(7));
        final List<PacsAvailability> pacs1Availabilities = _service.findAllByPacsId(1);
        assertThat(pacs1Availabilities).isNotNull().isNotEmpty().hasSize(7);
        final List<PacsAvailability> pacs1MondayAvailabilities = _service.findAllByPacsIdAndDayOfWeek(2, DayOfWeek.MONDAY);
        assertThat(pacs1MondayAvailabilities).isNotNull().isNotEmpty().hasSize(1);
        final Map<DayOfWeek, List<PacsAvailability>> pacs1GroupedAvailabilities = _service.findAllByPacsIdGroupedByDayOfWeek(1);
        assertThat(pacs1GroupedAvailabilities).isNotNull().isNotEmpty().hasSize(7).containsOnlyKeys(DayOfWeek.values()).allSatisfy((dayOfWeek, pacsAvailabilities) -> assertThat(pacsAvailabilities).hasSize(1));
    }

    @Test
    public void testOverlapFunctions() {
        final LocalDateTime    now           = LocalDateTime.now();
        final DayOfWeek        dayOfWeek     = now.getDayOfWeek();
        final PacsAvailability availability1 = _service.create(PacsAvailability.builder().pacsId(6L).dayOfWeek(dayOfWeek).threads(4).utilizationPercent(100).availabilityStart("00:00").availabilityEnd("12:00").build());
        final PacsAvailability availability2 = _service.create(PacsAvailability.builder().pacsId(6L).dayOfWeek(dayOfWeek).threads(4).utilizationPercent(100).availabilityStart("10:00").availabilityEnd("23:59").build());

        final boolean overlap1 = _service.checkOverlap(availability1, false);
        assertThat(overlap1).isTrue();
        final boolean overlap2 = _service.checkOverlap(availability2, false);
        assertThat(overlap2).isTrue();
        availability2.setAvailabilityStart("12:00");
        _service.update(availability2);
        final boolean overlap3 = _service.checkOverlap(availability1, false);
        assertThat(overlap3).isFalse();
        final boolean overlap4 = _service.checkOverlap(availability2, false);
        assertThat(overlap4).isFalse();

        final List<PacsAvailability> todays = _service.findAllByPacsIdAndDayOfWeek(6, dayOfWeek);
        assertThat(todays).isNotNull().isNotEmpty().hasSize(2);

        assertThat(availability1.isAvailableNow() || availability2.isAvailableNow()).isTrue();
        assertThat(availability1.isAvailableAtTime(LocalTime.of(8, 0))).isTrue();
        assertThat(availability1.isAvailableAtTime(LocalTime.of(20, 0))).isFalse();
        assertThat(availability2.isAvailableAtTime(LocalTime.of(8, 0))).isFalse();
        assertThat(availability2.isAvailableAtTime(LocalTime.of(20, 0))).isTrue();
    }

    private final PacsAvailabilityService _service;
}
