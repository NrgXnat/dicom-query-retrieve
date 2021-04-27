package org.nrg.xnatx.dqr.utils;

import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;

import lombok.Builder;
import lombok.Getter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.ObjectUtils;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.nrg.xnat.utils.DateRange;

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
public class DqrDateRange extends DateRange {
    public static final DateTimeFormatter HH_MM_FORMATTER = new DateTimeFormatterBuilder().appendValue(HOUR_OF_DAY, 2).appendLiteral(':').appendValue(MINUTE_OF_HOUR, 2).toFormatter();

    /**
     * Used by the {@link #relative(DqrDateRange)} method to indicate the relative position of the availability
     * window (that is, the time from {@link #getStartDate()} to {@link #getEndDate()}) of this object compared to
     * another:
     *
     * <ul>
     *     <li>
     *         <b>None</b> indicates that this object has the same start and end time as the other.
     *     </li>
     *     <li>
     *         <b>Before</b> indicates that this object comes before the other, with no overlap: {@link
     *         #getEndDate()} occurs before the other object's {@link #getStartDate()}
     *     </li>
     *     <li>
     *         <b>After</b> indicates that this object comes after the other, with no overlap: {@link
     *         #getStartDate()} occurs after the other object's {@link #getEndDate()}
     *     </li>
     *     <li>
     *         <b>Includes</b> indicates that this object includes the other: {@link #getStartDate()} occurs
     *         before the other object's {@link #getStartDate()} and {@link #getEndDate()} occurs
     *         after the other object's {@link #getEndDate()}
     *     </li>
     *     <li>
     *         <b>Included</b> indicates that the other object includes this object: {@link #getStartDate()}
     *         occurs after the other object's {@link #getStartDate()} and {@link #getEndDate()} occurs
     *         before the other object's {@link #getEndDate()}
     *     </li>
     *     <li>
     *         <b>BeforeOverlap</b> indicates that the time windows overlap, but this object's time window is earlier:
     *         {@link #getStartDate()} occurs before the other object's {@link #getStartDate()}, while
     *         {@link #getEndDate()} occurs after the other object's {@link #getStartDate()} but before
     *         the other object's {@link #getEndDate()}
     *     </li>
     *     <li>
     *         <b>AfterOverlap</b> indicates that the time windows overlap, but this object's time window is later:
     *         {@link #getStartDate()} occurs after the other object's {@link #getStartDate()} but
     *         before the other object's {@link #getEndDate()}, while {@link #getEndDate()} occurs after
     *         the other object's {@link #getEndDate()}
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

    public DqrDateRange(final Date startDate, final Date endDate) {
        this(convertDateToLocalDateTime(startDate), convertDateToLocalDateTime(endDate));
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

    public DqrDateRange(final String startDate, final String endDate) {
        this(convertDateToLocalDateTime(startDate), convertDateToLocalDateTime(endDate));
    }

    public DqrDateRange(final LocalTime startDate, final LocalTime endDate) {
        this(startDate, endDate, LocalDate.now().getDayOfWeek());
    }

    public DqrDateRange(final LocalTime startDate, final LocalTime endDate, final DayOfWeek dayOfWeek) {
        this(LocalDateTime.of(LocalDateTime.now().with(TemporalAdjusters.next(dayOfWeek)).toLocalDate(), startDate),
             LocalDateTime.of(LocalDateTime.now().with(TemporalAdjusters.next(dayOfWeek)).toLocalDate(), endDate));
    }

    public DqrDateRange(final LocalDateTime startDate, final LocalDateTime endDate) {
        _startDate = startDate == null ? LocalDateTime.MIN : startDate;
        final LocalDateTime initialEnd = endDate == null ? LocalDateTime.MAX : endDate;
        _endDate = _startDate.isBefore(initialEnd) ? initialEnd : initialEnd.plusDays(1);
    }

    public static LocalDateTime parse(final String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        try {
            if (Pattern.matches(SLASHY_PATTERN, date)) {
                return LocalDateTime.parse(date, SLASHY_FORMATTER);
            }
            if (Pattern.matches(DASHY_PATTERN, date)) {
                return LocalDateTime.parse(date, DASHY_FORMATTER);
            }
            if (Pattern.matches(BASIC_PATTERN, date)) {
                return LocalDateTime.parse(date, BASIC_FORMATTER);
            }
        } catch (DateTimeParseException ignored) {
        }
        throw new RuntimeException(String.format(PARSE_ERROR, date));
    }

    public static String formatLocalTime(final LocalDateTime date) {
        return formatLocalTime(date.toLocalTime());
    }

    public static String formatLocalTime(final LocalTime date) {
        return HH_MM_FORMATTER.format(ObjectUtils.defaultIfNull(date, LocalDateTime.MIN));
    }

    public static String formatDate(final Date date) {
        return DASHY_FORMATTER.format(convertDateToLocalDateTime(date));
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

    public boolean isEmpty() {
        return _startDate.isAfter(_endDate);
    }

    public boolean isBoundedAtStart() {
        return !_startDate.equals(LocalDateTime.MIN);
    }

    public boolean isBoundedAtEnd() {
        return !_endDate.equals(LocalDateTime.MAX);
    }

    public boolean isBounded() {
        return isBoundedAtStart() || isBoundedAtEnd();
    }

    public boolean includes(final Date date) {
        return includes(convertDateToLocalDateTime(date));
    }

    public boolean includes(final LocalDateTime date) {
        return (_startDate.isBefore(date) || _startDate.isEqual(date)) && (_endDate.equals(date) || _endDate.isAfter(date));
    }

    public boolean includes(final DqrDateRange range) {
        return includes(range._startDate) && includes(range._endDate);
    }

    @SuppressWarnings("unused")
    public boolean overlaps(final DqrDateRange range) {
        return includes(range.getStartDate()) || includes(range.getEndDate());
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
        if (getStartDate().isAfter(other.getEndDate()) || getStartDate().equals(other.getEndDate())) {
            return Relative.After;
        }
        if (getEndDate().isBefore(other.getStartDate()) || getEndDate().equals(other.getStartDate())) {
            return Relative.Before;
        }
        if (getStartDate().isBefore(other.getStartDate()) && getEndDate().isBefore(other.getEndDate())) {
            return Relative.BeforeOverlap;
        }
        if (getStartDate().isAfter(other.getStartDate()) && getEndDate().isAfter(other.getEndDate())) {
            return Relative.AfterOverlap;
        }
        throw new DqrRuntimeException("I found a weird relation for two DqrDateRange objects. First starts at " + formatLocalTime(getStartDate()) + " and ends at " + formatLocalTime(getEndDate()) + ", while the other starts at " + formatLocalTime(other.getStartDate()) + " and ends at " + formatLocalTime(other.getEndDate()));
    }

    @Override
    public String toString() {
        if (isEmpty()) {
            return "Empty Date Range";
        }
        return _startDate.toString() + " - " + _endDate.toString();
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
        return new EqualsBuilder().appendSuper(super.equals(object)).append(getStartDate(), that.getStartDate()).append(getEndDate(), that.getEndDate()).isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37).appendSuper(super.hashCode()).append(getStartDate()).append(getEndDate()).toHashCode();
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

    private final static Date MIN_DATE = new Date(-5364640800000L); //January 1, 1800
    private final static Date MAX_DATE = new Date(32503701600000L); //January 1, 3000.

    private final LocalDateTime _startDate;
    private final LocalDateTime _endDate;
}
