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

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.*;
import java.util.Date;

@Entity
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class ExecutedPacsRequest extends PacsRequest {

    protected Date _executedTime;

    // for Hibernate
    public ExecutedPacsRequest() {
    }

    public Date getExecutedTime() {
        return _executedTime;
    }

    public void setExecutedTime(Date _executedTime) {
        this._executedTime = _executedTime;
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(137, 479).append(_username).append(_pacsId)
                .append(_xnatProject).append(_studyInstanceUid).append(_seriesIds).append(_remappingScript).append(_destinationAeTitle).append(_status)
                .append(_queuedTime).append(_executedTime).append(_priority).append(_studyDate).append(_studyId)
                .append(_accessionNumber).append(_patientId).append(_patientName).toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final ExecutedPacsRequest other = (ExecutedPacsRequest) obj;
        return new EqualsBuilder().append(_username, other._username).append(_pacsId, other._pacsId)
                .append(_xnatProject, other._xnatProject).append(_studyInstanceUid, other._studyInstanceUid)
                .append(_seriesIds, other._seriesIds).append(_remappingScript, other._remappingScript)
                .append(_destinationAeTitle, other._destinationAeTitle).append(_status, other._status)
                .append(_queuedTime, other._queuedTime).append(_executedTime, other._executedTime)
                .append(_priority, other._priority).append(_studyDate, other._studyDate)
                .append(_studyId, other._studyId).append(_accessionNumber, other._accessionNumber)
                .append(_patientId, other._patientId).append(_patientName, other._patientName).isEquals();
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder("{ ");
        buffer.append("username: ").append(_username).append(", ");
        buffer.append("pacsId: ").append(_pacsId).append(", ");
        buffer.append("xnatProject: ").append(_xnatProject).append(", ");
        buffer.append("studyInstanceUid: ").append(_studyInstanceUid).append(", ");
        buffer.append("seriesIds: ").append(_seriesIds).append(", ");
        buffer.append("remappingScript: ").append(_remappingScript).append(", ");
        buffer.append("destinationAeTitle: ").append(_destinationAeTitle).append(", ");
        buffer.append("status: ").append(_status).append(", ");
        buffer.append("queuedTime: ").append(_queuedTime).append(", ");
        buffer.append("executedTime: ").append(_executedTime).append(", ");
        buffer.append("priority: ").append(_priority).append(", ");
        buffer.append("studyDate: ").append(_studyDate).append(", ");
        buffer.append("studyId: ").append(_studyId).append(", ");
        buffer.append("accessionNumber: ").append(_accessionNumber).append(", ");
        buffer.append("patientId: ").append(_patientId).append(", ");
        buffer.append("patientName: ").append(_patientName).append(", ");
        return buffer.toString();
    }
}
