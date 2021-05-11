/*
 * Pacs
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.time.DayOfWeek;
import java.time.LocalTime;
import java.util.Objects;
import java.util.regex.Pattern;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"pacsId", "dayOfWeek", "availabilityStart"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(prefix = "_")
public class PacsAvailability extends AbstractHibernateEntity {
    private static final long serialVersionUID = -5580029561175463693L;

    public long getPacsId() {
        return _pacsId;
    }

    public void setPacsId(final Long pacsId) {
        _pacsId = pacsId;
    }

    public DayOfWeek getDayOfWeek() {
        return _dayOfWeek;
    }

    public void setDayOfWeek(final DayOfWeek dayOfWeek) {
        _dayOfWeek = dayOfWeek;
    }

    @NotBlank
    @Size(max = 100)
    public String getAvailabilityStart() {
        return formatTime(_availabilityStart);
    }

    public void setAvailabilityStart(final String availabilityStart) {
        _availabilityStart = validateTime(availabilityStart);
    }

    @NotBlank
    @Size(max = 100)
    public String getAvailabilityEnd() {
        return formatTime(_availabilityEnd);
    }

    public void setAvailabilityEnd(final String availabilityEnd) {
        _availabilityEnd = validateTime(availabilityEnd);
    }

    public int getThreads() {
        return _threads;
    }

    public void setThreads(final int threads) {
        _threads = threads;
    }

    public int getUtilizationPercent() {
        return _utilizationPercent;
    }

    public void setUtilizationPercent(final int utilizationPercent) {
        _utilizationPercent = utilizationPercent;
    }

    @Transient
    public boolean isAvailableNow() {
        return isAvailableAtTime(LocalTime.now());
    }

    @Transient
    public boolean isAvailableAtTime(final LocalTime time) {
        final LocalTime start = LocalTime.parse(getAvailabilityStart());
        final LocalTime end   = StringUtils.equals(getAvailabilityEnd(), "00:00") ? LocalTime.MAX : LocalTime.parse(getAvailabilityEnd());
        return start.isBefore(end) ? time.isAfter(start) && time.isBefore(end) : time.isAfter(end) && time.isBefore(start);
    }

    public DqrDateRange.Relative relative(final PacsAvailability other) {
        return new DqrDateRange(getAvailabilityStart(), getAvailabilityEnd()).relative(new DqrDateRange(other.getAvailabilityStart(), other.getAvailabilityEnd()));
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        if (!super.equals(o)) {
            return false;
        }

        PacsAvailability that = (PacsAvailability) o;

        if (!Objects.equals(_pacsId, that._pacsId)) {
            return false;
        }
        if (_dayOfWeek != that._dayOfWeek) {
            return false;
        }
        if (!Objects.equals(_availabilityStart, that._availabilityStart)) {
            return false;
        }
        if (!Objects.equals(_availabilityEnd, that._availabilityEnd)) {
            return false;
        }
        if (_threads != that._threads) {
            return false;
        }
        return _utilizationPercent == that._utilizationPercent;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + Long.hashCode(_pacsId);
        result = 31 * result + _dayOfWeek.getValue();
        result = 31 * result + (_availabilityStart != null ? _availabilityStart.hashCode() : 0);
        result = 31 * result + (_availabilityEnd != null ? _availabilityEnd.hashCode() : 0);
        result = 31 * result + _threads;
        result = 31 * result + _utilizationPercent;
        return result;
    }

    @Override
    public String toString() {
        return String.format(FORMAT, getId(), _pacsId, _dayOfWeek, _availabilityStart, _availabilityEnd, _threads, _utilizationPercent);
    }

    private static String formatTime(final String time) {
        return StringUtils.leftPad(time, 5, '0');
    }

    private static String validateTime(final String time) {
        if (TIME_OF_DAY.matcher(time).matches()) {
            return time;
        }
        if (SHORT_TIME_OF_DAY.matcher(time).matches()) {
            return "0" + time;
        }
        if (StringUtils.equals(time, BAD_MIDNIGHT)) {
            return GOOD_MIDNIGHT;
        }
        throw new RuntimeException("The value given, " + time + ", is invalid, must be format \"hh:mm\" from \"00:00\" to \"23:59\", i.e. \"hh\" is any value between 0 and 23 (single digits should be padded with a leading zero) and \"mm\" is any value between 00 and 59.");
    }

    private static final String  FORMAT            = "{ \"id\": %s, \"pacsId\": %s, \"dayOfWeek\": \"%s\", \"availabilityStart\": \"%s\", \"availabilityEnd\": \"%s\", \"threads\": %d, \"utilizationPercent\": %d }";
    private static final Pattern TIME_OF_DAY       = Pattern.compile("^(?:[01][0-9]|2[0-3]):[0-5][0-9]$");
    private static final Pattern SHORT_TIME_OF_DAY = Pattern.compile("^[0-9]:[0-5][0-9]$");
    private static final String  BAD_MIDNIGHT      = "24:00";
    private static final String  GOOD_MIDNIGHT     = "00:00";

    private long      _pacsId;
    private DayOfWeek _dayOfWeek;
    private String    _availabilityStart;
    private String    _availabilityEnd;
    private int       _threads;
    private int       _utilizationPercent;
}
