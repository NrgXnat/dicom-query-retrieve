package org.nrg.xnatx.dqr.utils;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.nrg.xnatx.dqr.exceptions.DqrRuntimeException;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.time.format.DateTimeParseException;
import java.time.temporal.TemporalAdjusters;
import java.util.Date;
import java.util.regex.Pattern;

@Getter
@Accessors(prefix = "_")
@Builder
public class DqrDateRange {
    public static final DateTimeFormatter HH_MM_FORMATTER = new DateTimeFormatterBuilder().appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).toFormatter();

    /**
     * Used by the {@link #relative(DqrDateRange)} method to indicate the relative position of the availability
     * window (that is, the time from {@link #getStart()} to {@link #getEnd()}) of this object compared to
     * another:
     *
     * <ul>
     *     <li>
     *         <b>None</b> indicates that this object has the same start and end time as the other.
     *     </li>
     *     <li>
     *         <b>Before</b> indicates that this object comes before the other, with no overlap: {@link
     *         #getEnd()} occurs before the other object's {@link #getStart()}
     *     </li>
     *     <li>
     *         <b>After</b> indicates that this object comes after the other, with no overlap: {@link
     *         #getStart()} occurs after the other object's {@link #getEnd()}
     *     </li>
     *     <li>
     *         <b>Includes</b> indicates that this object includes the other: {@link #getStart()} occurs
     *         before the other object's {@link #getStart()} and {@link #getEnd()} occurs
     *         after the other object's {@link #getEnd()}
     *     </li>
     *     <li>
     *         <b>Included</b> indicates that the other object includes this object: {@link #getStart()}
     *         occurs after the other object's {@link #getStart()} and {@link #getEnd()} occurs
     *         before the other object's {@link #getEnd()}
     *     </li>
     *     <li>
     *         <b>BeforeOverlap</b> indicates that the time windows overlap, but this object's time window is earlier:
     *         {@link #getStart()} occurs before the other object's {@link #getStart()}, while
     *         {@link #getEnd()} occurs after the other object's {@link #getStart()} but before
     *         the other object's {@link #getEnd()}
     *     </li>
     *     <li>
     *         <b>AfterOverlap</b> indicates that the time windows overlap, but this object's time window is later:
     *         {@link #getStart()} occurs after the other object's {@link #getStart()} but
     *         before the other object's {@link #getEnd()}, while {@link #getEnd()} occurs after
     *         the other object's {@link #getEnd()}
     *     </li>
     * </ul>
     */
    public enum Relative {
        Identical,
        Before,
        After,
        Includes,
        Included,
        BeforeOverlap,
        AfterOverlap
    }

    /**
     * represents a date range that is unbounded/infinite on both ends.
     */
    public DqrDateRange() {
        this(LocalDateTime.MIN, LocalDateTime.MAX);
    }

    /**
     * Creates a new date range that starts with the date contained in the <b>start</b>
     * parameter and ends one day later.
     *
     * @param start The start date in text format.
     */
    public DqrDateRange(final String start) {
        this(convertDateToLocalDateTime(start), convertDateToLocalDateTime(start, 1));
    }

    public DqrDateRange(final String start, final String end) {
        this(convertDateToLocalDateTime(start), convertDateToLocalDateTime(end));
    }

    public DqrDateRange(final LocalTime start, final LocalTime end) {
        this(start, end, LocalDate.now().getDayOfWeek());
    }

    public DqrDateRange(final LocalTime start, final LocalTime end, final DayOfWeek dayOfWeek) {
        this(LocalDateTime.of(LocalDateTime.now().with(TemporalAdjusters.next(dayOfWeek)).toLocalDate(), start),
             LocalDateTime.of(LocalDateTime.now().with(TemporalAdjusters.next(dayOfWeek)).toLocalDate(), end));
    }

    public DqrDateRange(final LocalDateTime start, final LocalDateTime end) {
        _start = start == null ? LocalDateTime.MIN : start;
        final LocalDateTime initialEnd = end == null ? LocalDateTime.MAX : end;
        _end = _start.isBefore(initialEnd) ? initialEnd : initialEnd.plusDays(1);
    }

    public static LocalDateTime parse(final String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        try {
            if (Pattern.matches(SLASHY_PATTERN, date)) {
                return LocalDate.parse(date, SLASHY_FORMATTER).atStartOfDay();
            }
            if (Pattern.matches(DASHY_PATTERN, date)) {
                return LocalDate.parse(date, DASHY_FORMATTER).atStartOfDay();
            }
            if (Pattern.matches(BASIC_PATTERN, date)) {
                return LocalDate.parse(date, BASIC_FORMATTER).atStartOfDay();
            }
        } catch (DateTimeParseException ignored) {
        }
        throw new RuntimeException(String.format(PARSE_ERROR, date));
    }

    public static String formatDate(final LocalDateTime date) {
        return formatLocalTime(date.toLocalTime());
    }

    public static String formatLocalTime(final LocalTime date) {
        return HH_MM_FORMATTER.format(ObjectUtils.defaultIfNull(date, LocalDateTime.MIN));
    }

    public static String formatDate(final Date date) {
        return DASHY_FORMATTER.format(convertDateToLocalDateTime(date));
    }

    public static String formatDicomDate(final LocalDateTime date) {
        return BASIC_FORMATTER.format(date);
    }

    public static Pair<LocalDateTime, LocalDateTime> getDateRange(final LocalTime start, final LocalTime end) {
        return getDateRange(start, end, LocalDate.now().getDayOfWeek());
    }

    public static Pair<LocalDateTime, LocalDateTime> getDateRange(final LocalTime start, final LocalTime end, final DayOfWeek dayOfWeek) {
        final LocalDateTime now  = LocalDateTime.now();
        final LocalDate     date = now.getDayOfWeek() == dayOfWeek ? now.toLocalDate() : now.with(TemporalAdjusters.next(dayOfWeek)).toLocalDate();
        if (start.isBefore(end) || start.equals(end)) {
            return Pair.of(LocalDateTime.of(date, start), LocalDateTime.of(date, end));
        }
        return Pair.of(LocalDateTime.of(date, start), LocalDateTime.of(date.plusDays(1), end));
    }

    @JsonIgnore
    public boolean isEmpty() {
        return _start.isAfter(_end);
    }

    @JsonIgnore
    public boolean isBoundedAtStart() {
        return !_start.equals(LocalDateTime.MIN);
    }

    @JsonIgnore
    public boolean isBoundedAtEnd() {
        return !_end.equals(LocalDateTime.MAX);
    }

    @JsonIgnore
    public boolean isBounded() {
        return !isEmpty() && (isBoundedAtStart() || isBoundedAtEnd());
    }

    public boolean includes(final LocalDateTime date) {
        return (_start.isBefore(date) || _start.isEqual(date)) && (_end.equals(date) || _end.isAfter(date));
    }

    public boolean includes(final DqrDateRange range) {
        return includes(range._start) && includes(range._end);
    }

    @SuppressWarnings("unused")
    public boolean overlaps(final DqrDateRange range) {
        return includes(range.getStart()) || includes(range.getEnd());
    }

    public Relative relative(final DqrDateRange other) {
        if (equals(other)) {
            return Relative.Identical;
        }
        if (includes(other)) {
            return Relative.Includes;
        }
        if (other.includes(this)) {
            return Relative.Included;
        }
        if (getStart().isAfter(other.getEnd()) || getStart().equals(other.getEnd())) {
            return Relative.After;
        }
        if (getEnd().isBefore(other.getStart()) || getEnd().equals(other.getStart())) {
            return Relative.Before;
        }
        if (getStart().isBefore(other.getStart()) && getEnd().isBefore(other.getEnd())) {
            return Relative.BeforeOverlap;
        }
        if (getStart().isAfter(other.getStart()) && getEnd().isAfter(other.getEnd())) {
            return Relative.AfterOverlap;
        }
        throw new DqrRuntimeException("I found a weird relation for two DqrDateRange objects. First starts at " + formatDate(getStart()) + " and ends at " + formatDate(getEnd()) + ", while the other starts at " + formatDate(other.getStart()) + " and ends at " + formatDate(other.getEnd()));
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "Empty Date Range";
        }
        return _start.toString() + " - " + _end.toString();
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        final DqrDateRange that = (DqrDateRange) object;
        return new EqualsBuilder().appendSuper(super.equals(object)).append(getStart(), that.getStart()).append(getEnd(), that.getEnd()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(getStart()).append(getEnd()).toHashCode();
    }

    private static LocalDateTime convertDateToLocalDateTime(final Date date) {
        return date == null ? null : date.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
    }

    private static LocalDateTime convertDateToLocalDateTime(final String date) {
        return StringUtils.isBlank(date) ? null : LocalDateTime.of(LocalDate.now(), LocalTime.parse(date));
    }

    @SuppressWarnings("SameParameterValue")
    private static LocalDateTime convertDateToLocalDateTime(final String date, final int increment) {
        return StringUtils.isBlank(date) ? null : LocalDateTime.of(LocalDate.now(), LocalTime.parse(date)).plusDays(increment);
    }

    private static final DateTimeFormatter BASIC_FORMATTER  = DateTimeFormatter.ofPattern("yyyyMMdd");
    private static final DateTimeFormatter SLASHY_FORMATTER = DateTimeFormatter.ofPattern("yyyy/MM/dd");
    private static final DateTimeFormatter DASHY_FORMATTER  = DateTimeFormatter.ofPattern("yyyy-MM-dd");
    private static final String            BASIC_PATTERN    = "^\\d{8}$";
    private static final String            SLASHY_PATTERN   = "^\\d{4}/\\d{2}/\\d{2}";
    private static final String            DASHY_PATTERN    = "^\\d{8}-\\d{2}-\\d{2}";
    private static final String            PARSE_ERROR      = "An error occurred parsing the date value \"%s\": should match one of the patterns \"" + BASIC_PATTERN + "\", \"" + SLASHY_PATTERN + "\", or \"" + DASHY_PATTERN + "\"";

    private final LocalDateTime _start;
    private final LocalDateTime _end;
}
