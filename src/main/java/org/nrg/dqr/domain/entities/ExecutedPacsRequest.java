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

import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;

@Entity
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@NoArgsConstructor
public class ExecutedPacsRequest extends PacsRequest {
    @Temporal(TemporalType.TIMESTAMP)
    public Date getExecutedTime() {
        return _executedTime;
    }

    public void setExecutedTime(final Date executedTime) {
        _executedTime = executedTime;
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final ExecutedPacsRequest request = (ExecutedPacsRequest) other;

        return new EqualsBuilder()
                .appendSuper(super.equals(other))
                .append(getExecutedTime(), request.getExecutedTime())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(getExecutedTime())
                .toHashCode();
    }

    @Override
    public String toString() {
        return String.format(FORMAT, getClass().getName(), getUsername(), getPacsId(), getXnatProject(), getStudyInstanceUid(), getSeriesIds(), getRemappingScript(), getDestinationAeTitle(), getStatus(), getQueuedTime(), getExecutedTime(), getPriority(), getStudyDate(), getStudyId(), getAccessionNumber(), getPatientId(), getPatientName());
    }

    private static final String FORMAT = "{ \"type\": \"%s\", \"username\": \"%s\", \"pacsId\": %d, \"xnatProject\": \"%s\", \"studyInstanceUid\": \"%s\", \"seriesIds\": \"%s\", \"remappingScript\": \"%s\", \"destinationAeTitle\": \"%s\", \"status\": \"%s\", \"queuedTime\": \"%s\", \"executedTime\": \"%s\", \"priority\": %d, \"studyDate\": \"%s\", \"studyId\": \"%s\", \"accessionNumber\": \"%s\", \"patientId\": \"%s\", \"patientName\": \"%s\": }";

    protected Date _executedTime;
}
