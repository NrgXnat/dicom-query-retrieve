/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.utils.DqrDateRange
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.utils;

import com.fasterxml.jackson.annotation.JsonIgnore;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.TimeZone;
import java.util.regex.Pattern;
import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.Getter;
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

@Getter
@Accessors(prefix = "_")
@EqualsAndHashCode(of = {"_start", "_end"})
@Builder
@Slf4j
public class DqrDateRange {
    /**
     * represents a date range that is unbounded/infinite on both ends.
     */
    public DqrDateRange() {
        this(MIN_DATE, MAX_DATE);
    }

    public DqrDateRange(final Date start, final Date end) {
        _start = new Date((start != null ? start : MIN_DATE).getTime());
        _end = new Date((end != null ? end : MAX_DATE).getTime());
    }

    /**
     * Creates a new date range that starts with the date contained in the <b>start</b>
     * parameter and ends with the date contained in the <b>end</b> parameter.
     *
     * @param start The start date in text format.
     * @param end   The end date in text format.
     */
    public DqrDateRange(final String start, final String end) {
        this(parse(start), parse(end));
    }

    /**
     * Creates a new date range that starts with the date contained in the <b>start</b>
     * parameter and ends one day later.
     *
     * @param start The start date in text format.
     */
    public DqrDateRange(final String start) {
        this(parse(start), 1);
    }

    /**
     * Creates a new date range that starts with the date contained in the <b>start</b>
     * parameter and ends <b>days</b> day later.
     *
     * @param start The start date in text format.
     * @param days  The number of days in the date range.
     */
    public DqrDateRange(final String start, final int days) {
        this(parse(start), days);
    }

    /**
     * Creates a new date range that starts with the <b>start</b> date parameter and
     * ends <b>days</b> day later.
     *
     * @param start The start date.
     * @param days  The number of days in the date range.
     */
    public DqrDateRange(final Date start, final int days) {
        this(start, getDatePlusDays(start, days));
    }

    public static Date parse(final String date) {
        if (StringUtils.isBlank(date)) {
            return null;
        }
        try {
            if (Pattern.matches(SLASHY_PATTERN, date)) {
                return SLASHY_FORMATTER.parse(date);
            }
            if (Pattern.matches(DASHY_PATTERN, date)) {
                return DASHY_FORMATTER.parse(date);
            }
            if (Pattern.matches(BASIC_PATTERN, date)) {
                return BASIC_FORMATTER.parse(date);
            }
        } catch (ParseException ignored) {
        }
        throw new RuntimeException(String.format(PARSE_ERROR, date));
    }

    public static String format(final Date date) {
        return BASIC_FORMATTER.format(date);
    }

    @JsonIgnore
    public boolean isEmpty() {
        return _start.after(_end);
    }

    @JsonIgnore
    public boolean isBoundedAtStart() {
        return !_start.equals(MIN_DATE);
    }

    @JsonIgnore
    public boolean isBoundedAtEnd() {
        return !_end.equals(MAX_DATE);
    }

    @JsonIgnore
    public boolean isBounded() {
        return isBoundedAtStart() || isBoundedAtEnd();
    }

    @Override
    public String toString() {
        return isEmpty() ? "Empty Date Range" : BASIC_FORMATTER.format(_start) + " - " + BASIC_FORMATTER.format(_end);
    }

    public boolean includes(final Date arg) {
        return !arg.before(_start) && !arg.after(_end);
    }

    public boolean includes(final DqrDateRange arg) {
        return this.includes(arg._start) && this.includes(arg._end);
    }

    @SuppressWarnings("unused")
    public boolean overlaps(final DqrDateRange arg) {
        return arg.includes(_start) || arg.includes(_end) || this.includes(arg);
    }

    private static Date getDatePlusDays(final Date date, final int days) {
        final Calendar calendar = Calendar.getInstance();
        calendar.setTime(date);
        calendar.add(Calendar.DATE, days);
        return calendar.getTime();
    }

    private static final DateFormat BASIC_FORMATTER  = new SimpleDateFormat("yyyyMMdd");
    private static final DateFormat SLASHY_FORMATTER = new SimpleDateFormat("yyyy/MM/dd");
    private static final DateFormat DASHY_FORMATTER  = new SimpleDateFormat("yyyy-MM-dd");
    private static final String     BASIC_PATTERN    = "^\\d{8}$";
    private static final String     SLASHY_PATTERN   = "^\\d{4}/\\d{2}/\\d{2}";
    private static final String     DASHY_PATTERN    = "^\\d{8}-\\d{2}-\\d{2}";
    private static final String     PARSE_ERROR      = "An error occurred parsing the date value \"%s\": should match one of the patterns \"" + BASIC_PATTERN + "\", \"" + SLASHY_PATTERN + "\", or \"" + DASHY_PATTERN + "\"";

    static {
        BASIC_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
        SLASHY_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
        DASHY_FORMATTER.setTimeZone(TimeZone.getTimeZone("GMT"));
    }

    private final static Date MIN_DATE = new Date(-5364640800000L); //January 1, 1800
    private final static Date MAX_DATE = new Date(32503701600000L); //January 1, 3000.

    private final Date _start;
    private final Date _end;
}
