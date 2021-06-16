package org.nrg.xnatx.dqr.utils;

import org.apache.commons.lang3.RegExUtils;
import org.apache.commons.lang3.StringUtils;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.IntStream;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;
import static org.nrg.xnatx.dqr.domain.entities.PacsAvailability.BAD_MIDNIGHT;

public class TestDqrDateRange {
    @Test
    public void testParseMethod() {
        final List<LocalDateTime> dates = Stream.of(BASIC_DATE, DASHY_DATE, SLASHY_DATE).map(DqrDateRange::parse).collect(Collectors.toList());
        assertThat(dates).isNotNull().isNotEmpty().containsOnly(REFERENCE);
        Assertions.assertThrows(DqrRuntimeException.class, () -> DqrDateRange.parse(null), DqrDateRange.NO_DATE_ERROR);
        Assertions.assertThrows(DqrRuntimeException.class, () -> DqrDateRange.parse(BAD_DATE_1), String.format(DqrDateRange.PARSE_ERROR, BAD_DATE_1));
        Assertions.assertThrows(DqrRuntimeException.class, () -> DqrDateRange.parse(BAD_DATE_2), String.format(DqrDateRange.PARSE_ERROR, BAD_DATE_2));
    }

    @Test
    public void testPatterns() {
        // Basic date tests
        final Map<String, Boolean> results = DATES.stream().collect(Collectors.toMap(Function.identity(), date -> DqrDateRange.DATE_PATTERN.matcher(date).matches()));
        assertThat(results).hasSize(5).containsOnlyKeys(BASIC_DATE, DASHY_DATE, SLASHY_DATE, BAD_DATE_1, BAD_DATE_2).containsValues(true, true, true, false, false);

        // Test all possible times during the day. This is a little bit silly but let's make sure the regex patterns match properly.
        final List<Boolean> matches = Stream.concat(ALL_TIMES.stream(), Stream.of(BAD_MIDNIGHT)).map(date -> DqrDateRange.TIME_PATTERN.matcher(date).matches()).distinct().collect(Collectors.toList());
        assertThat(matches).hasSize(1).containsOnly(true);
        final List<Boolean> fails = BAD_TIMES.stream().map(date -> DqrDateRange.TIME_PATTERN.matcher(date).matches()).distinct().collect(Collectors.toList());
        assertThat(fails).hasSize(1).containsOnly(false);

        // Now test all dates and times
        final Map<String, Boolean> dateTimeMatches = DATES.stream().map(date -> ALL_TIMES.stream().map(time -> date + " " + time).collect(Collectors.toList())).flatMap(Collection::stream).collect(Collectors.toMap(Function.identity(), dateTime -> DqrDateRange.DATE_TIME_PATTERN.matcher(dateTime).matches()));
        final List<Boolean>        goodDates       = dateTimeMatches.keySet().stream().filter(key -> StringUtils.startsWithAny(key, BASIC_DATE, DASHY_DATE, SLASHY_DATE)).map(dateTimeMatches::get).distinct().collect(Collectors.toList());
        final List<Boolean>        badDates        = dateTimeMatches.keySet().stream().filter(key -> StringUtils.startsWithAny(key, BAD_DATE_1, BAD_DATE_2)).map(dateTimeMatches::get).distinct().collect(Collectors.toList());
        assertThat(goodDates).hasSize(1).containsOnly(true);
        assertThat(badDates).hasSize(1).containsOnly(false);
    }

    private static final String        BASIC_DATE    = "20210616";
    private static final String        DASHY_DATE    = "2021-06-16";
    private static final String        SLASHY_DATE   = "2021/06/16";
    private static final String        BAD_DATE_1    = "2021_06_16";
    private static final String        BAD_DATE_2    = "06/16/2021";
    private static final List<String>  DATES         = Arrays.asList(BASIC_DATE, DASHY_DATE, SLASHY_DATE, BAD_DATE_1, BAD_DATE_2);
    private static final List<String>  ALL_RAW_TIMES = Stream.concat(IntStream.range(0, 24).boxed().map(Object::toString), IntStream.range(0, 10).boxed().map(hour -> "0" + hour))
                                                             .map(hour -> IntStream.range(0, 60)
                                                                                   .boxed()
                                                                                   .map(minute -> hour + ":" + (minute < 10 ? "0" + minute : minute))
                                                                                   .collect(Collectors.toList()))
                                                             .flatMap(Collection::stream)
                                                             .collect(Collectors.toList());
    private static final List<String>  ALL_TIMES     = Stream.concat(ALL_RAW_TIMES.stream(), ALL_RAW_TIMES.stream().map(time -> StringUtils.remove(time, ":"))).collect(Collectors.toList());
    private static final List<String>  BAD_TIMES     = Arrays.asList("1:4", "24:01", "flail", "", "32", "02302");
    private static final LocalDateTime REFERENCE     = LocalDate.parse(BASIC_DATE, DateTimeFormatter.ofPattern("yyyyMMdd")).atStartOfDay();
}
