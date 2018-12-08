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

package org.nrg.dqr.domain.entities;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.dqr.domain.entities.PortNotNullIfDefaultPacs;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import java.io.Serializable;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"pacsId","dayOfWeek","availabilityStart"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class PacsAvailability extends AbstractHibernateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private Long _pacsId;
    private int _dayOfWeek;
    private String _availabilityStart;
    private String _availabilityEnd;
    private int _sessionsPerHour;

    // for Hibernate
    public PacsAvailability() {
    }

    public Long getPacsId() {
        return _pacsId;
    }

    public void setPacsId(Long pacsId) {
        this._pacsId = pacsId;
    }

    public int getDayOfWeek() {
        return _dayOfWeek;
    }

    public void setDayOfWeek(int dayOfWeek) {
        this._dayOfWeek = dayOfWeek;
    }

    @NotBlank
    @Size(max = 100)
    public String getAvailabilityStart() {
        return _availabilityStart;
    }

    public void setAvailabilityStart(String availabilityStart) {
        this._availabilityStart = availabilityStart;
    }

    @NotBlank
    @Size(max = 100)
    public String getAvailabilityEnd() {
        return _availabilityEnd;
    }

    public void setAvailabilityEnd(String availabilityEnd) {
        this._availabilityEnd = availabilityEnd;
    }

    public int getSessionsPerHour() {
        return _sessionsPerHour;
    }

    public void setSessionsPerHour(int sessionsPerHour) {
        this._sessionsPerHour = sessionsPerHour;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        PacsAvailability that = (PacsAvailability) o;

        if (_pacsId != null ? !_pacsId.equals(that._pacsId) : that._pacsId != null) return false;
        if (_dayOfWeek != that._dayOfWeek) return false;
        if (_availabilityStart != null ? !_availabilityStart.equals(that._availabilityStart) : that._availabilityStart != null)
            return false;
        if (_availabilityEnd != null ? !_availabilityEnd.equals(that._availabilityEnd) : that._availabilityEnd != null)
            return false;
        return _sessionsPerHour==that._sessionsPerHour;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_pacsId != null ? _pacsId.hashCode() : 0);
        result = 31 * result + _dayOfWeek;
        result = 31 * result + (_availabilityStart != null ? _availabilityStart.hashCode() : 0);
        result = 31 * result + (_availabilityEnd != null ? _availabilityEnd.hashCode() : 0);
        result = 31 * result + _sessionsPerHour;
        return result;
    }

    @Override
    public String toString() {
        return "PacsAvailability{" +
                "pacsId=" + _pacsId +
                ", dayOfWeek='" + _dayOfWeek + '\'' +
                ", availabilityStart='" + _availabilityStart + '\'' +
                ", availabilityEnd='" + _availabilityEnd + '\'' +
                ", sessionsPerHour=" + _sessionsPerHour +
                '}';
    }
}
