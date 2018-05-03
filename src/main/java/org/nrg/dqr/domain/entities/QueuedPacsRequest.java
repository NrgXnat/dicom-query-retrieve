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

import javax.persistence.Entity;
import javax.persistence.Table;

@Entity
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class QueuedPacsRequest extends PacsRequest {

    // for Hibernate
    public QueuedPacsRequest() {
    }


    @Override
    public int hashCode() {
        return new HashCodeBuilder(137, 479).append(_username).append(_pacsId)
                .append(_xnatProject).append(_studyInstanceUid).append(_seriesIds).append(_destinationAeTitle)
                .append(_queuedTime).toHashCode();
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
        final QueuedPacsRequest other = (QueuedPacsRequest) obj;
        return new EqualsBuilder().append(_username, other._username).append(_pacsId, other._pacsId)
                .append(_xnatProject, other._xnatProject).append(_studyInstanceUid, other._studyInstanceUid)
                .append(_seriesIds, other._seriesIds).append(_destinationAeTitle, other._destinationAeTitle)
                .append(_queuedTime, other._queuedTime).isEquals();
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder("{ ");
        buffer.append("username: ").append(_username).append(", ");
        buffer.append("pacsId: ").append(_pacsId).append(", ");
        buffer.append("xnatProject: ").append(_xnatProject).append(", ");
        buffer.append("studyId: ").append(_studyInstanceUid).append(", ");
        buffer.append("seriesIds: ").append(_seriesIds).append(", ");
        buffer.append("destinationAeTitle: ").append(_destinationAeTitle).append(", ");
        buffer.append("queuedTime: ").append(_queuedTime).append(", ");
        return buffer.toString();
    }
}
