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
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class PacsPing extends AbstractHibernateEntity implements Serializable {
    protected static final long serialVersionUID = 1L;

    protected Long _pacsId;
    protected boolean _successful;
    protected Date _pingTime;

    public Long getPacsId() {
        return _pacsId;
    }

    public void setPacsId(Long _pacsId) {
        this._pacsId = _pacsId;
    }

    public boolean isSuccessful() {
        return _successful;
    }

    public void setSuccessful(boolean _successful) {
        this._successful = _successful;
    }

    public Date getPingTime() {
        return _pingTime;
    }

    public void setPingTime(Date _pingTime) {
        this._pingTime = _pingTime;
    }

    // for Hibernate
    public PacsPing() {
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(137, 479).append(_pacsId).append(_successful)
                .append(_pingTime).toHashCode();
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
        final PacsPing other = (PacsPing) obj;
        return new EqualsBuilder().append(_pacsId, other._pacsId).append(_successful, other._successful)
                .append(_pingTime, other._pingTime).isEquals();
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder("{ ");
        buffer.append("pacsId: ").append(_pacsId).append(", ");
        buffer.append("_successful: ").append(_successful).append(", ");
        buffer.append("pingTime: ").append(_pingTime).append(", ");
        return buffer.toString();
    }
}
