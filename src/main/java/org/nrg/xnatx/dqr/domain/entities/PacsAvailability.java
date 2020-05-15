/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.PacsAvailability
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import java.io.Serializable;
import java.util.Calendar;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Transient;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.jetbrains.annotations.NotNull;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"pacsId", "dayOfWeek", "availabilityStart"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class PacsAvailability extends AbstractHibernateEntity implements Serializable {
    public static long getAvailabilityTimeInMillis(final Calendar calendar, final String availabilityTime) {
        if (StringUtils.isNotBlank(availabilityTime)) {
            try {
                final Calendar cloned = (Calendar) calendar.clone();
                final String[] atoms  = StringUtils.split(availabilityTime, ":");
                cloned.set(Calendar.HOUR_OF_DAY, Integer.parseInt(atoms[0]));
                cloned.set(Calendar.MINUTE, Integer.parseInt(atoms[1]));
                return cloned.getTimeInMillis();
            } catch (Exception ignored) {

            }
        }
        return 0;
    }

    public Long getPacsId() {
        return _pacsId;
    }

    public void setPacsId(Long pacsId) {
        _pacsId = pacsId;
    }

    public int getDayOfWeek() {
        return _dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        _dayOfWeek = dayOfWeek;
    }

    @NotBlank
    @Size(max = 100)
    public String getAvailabilityStart() {
        return _availabilityStart;
    }

    public void setAvailabilityStart(final String availabilityStart) {
        _availabilityStart = zeroPadHour(availabilityStart);
    }

    @NotBlank
    @Size(max = 100)
    public String getAvailabilityEnd() {
        return _availabilityEnd;
    }

    public void setAvailabilityEnd(final String availabilityEnd) {
        _availabilityEnd = zeroPadHour(availabilityEnd);
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

    public void setUtilizationPercent(int utilizationPercent) {
        _utilizationPercent = utilizationPercent;
    }

    @Transient
    public boolean isAvailable(final Calendar calendar) {
        final int  currentDayOfWeek = calendar.get(Calendar.DAY_OF_WEEK);
        final int  availabilityDay  = getDayOfWeek();
        final long currentTime      = calendar.getTimeInMillis();
        final long startTime        = PacsAvailability.getAvailabilityTimeInMillis(calendar, getAvailabilityStart());
        final long endTime          = PacsAvailability.getAvailabilityTimeInMillis(calendar, getAvailabilityEnd());

        if (endTime < startTime) {
            //That means that the availability interval contains midnight.
            return (currentTime > startTime && currentDayOfWeek == availabilityDay) || (currentTime < endTime && currentDayOfWeek == (availabilityDay + 1));
        } else {
            return currentTime > startTime && currentTime < endTime && currentDayOfWeek == availabilityDay;
        }
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (!(object instanceof PacsAvailability) || !super.equals(object)) {
            return false;
        }

        final PacsAvailability that = (PacsAvailability) object;
        return Objects.equals(_pacsId, that._pacsId) &&
               _dayOfWeek == that._dayOfWeek &&
               StringUtils.equals(_availabilityStart, that._availabilityStart) &&
               StringUtils.equals(_availabilityEnd, that._availabilityEnd) &&
               that._threads == _threads &&
               _utilizationPercent == that._utilizationPercent;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_pacsId != null ? _pacsId.hashCode() : 0);
        result = 31 * result + _dayOfWeek;
        result = 31 * result + (_availabilityStart != null ? _availabilityStart.hashCode() : 0);
        result = 31 * result + (_availabilityEnd != null ? _availabilityEnd.hashCode() : 0);
        result = 31 * result + _threads;
        result = 31 * result + _utilizationPercent;
        return result;
    }

    @Override
    public String toString() {
        return "PacsAvailability{" +
               "pacsId=" + _pacsId +
               ", dayOfWeek='" + _dayOfWeek + '\'' +
               ", availabilityStart='" + _availabilityStart + '\'' +
               ", availabilityEnd='" + _availabilityEnd + '\'' +
               ", threads='" + _threads + '\'' +
               ", utilizationPercent=" + _utilizationPercent +
               '}';
    }

    @NotNull
    private static String zeroPadHour(final String time) {
        return time.charAt(1) == ':' ? "0" + time : time;
    }

    private Long   _pacsId;
    private int    _dayOfWeek;
    private String _availabilityStart;
    private String _availabilityEnd;
    private int    _threads;
    private int    _utilizationPercent;
}
