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

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Entity
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class PacsRequest extends AbstractHibernateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String _username;
    private Long _pacsId;
    private String _xnatProject;
    private String _studyId;
    private String _seriesIds;

    private String _destinationAeTitle;

    private Date _queuedTime;
    private Date _requestTime;

    public String getDestinationAeTitle() {
        return _destinationAeTitle;
    }

    public void setDestinationAeTitle(String _destinationAeTitle) {
        this._destinationAeTitle = _destinationAeTitle;
    }

    public Date getQueuedTime() {
        return _queuedTime;
    }

    public void setQueuedTime(Date _queuedTime) {
        this._queuedTime = _queuedTime;
    }

    public Date getRequestTime() {
        return _requestTime;
    }

    public void setRequestTime(Date _requestTime) {
        this._requestTime = _requestTime;
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(String _username) {
        this._username = _username;
    }

    public Long getPacsId() {
        return _pacsId;
    }

    public void setPacsId(Long _pacsId) {
        this._pacsId = _pacsId;
    }

    public String getXnatProject() {
        return _xnatProject;
    }

    public void setXnatProject(String _xnatProject) {
        this._xnatProject = _xnatProject;
    }

    public String getStudyId() {
        return _studyId;
    }

    public void setStudyId(String _studyId) {
        this._studyId = _studyId;
    }

    public String getSeriesIds() {
        return _seriesIds;
    }

    public void setSeriesIds(String _seriesIds) {
        this._seriesIds = _seriesIds;
    }

    // for Hibernate
    public PacsRequest() {
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(137, 479).append(_username).append(_pacsId)
                .append(_xnatProject).append(_studyId).append(_seriesIds).append(_destinationAeTitle)
                .append(_queuedTime).append(_requestTime).toHashCode();
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
        final PacsRequest other = (PacsRequest) obj;
        return new EqualsBuilder().append(_username, other._username).append(_pacsId, other._pacsId)
                .append(_xnatProject, other._xnatProject).append(_studyId, other._studyId)
                .append(_seriesIds, other._seriesIds).append(_destinationAeTitle, other._destinationAeTitle)
                .append(_queuedTime, other._queuedTime).append(_requestTime, other._requestTime).isEquals();
    }

    @Override
    public String toString() {
        final StringBuilder buffer = new StringBuilder("{ ");
        buffer.append("username: ").append(_username).append(", ");
        buffer.append("pacsId: ").append(_pacsId).append(", ");
        buffer.append("xnatProject: ").append(_xnatProject).append(", ");
        buffer.append("studyId: ").append(_studyId).append(", ");
        buffer.append("seriesIds: ").append(_seriesIds).append(", ");
        buffer.append("destinationAeTitle: ").append(_destinationAeTitle).append(", ");
        buffer.append("queuedTime: ").append(_queuedTime).append(", ");
        buffer.append("requestTime: ").append(_requestTime).append(", ");
        return buffer.toString();
    }
}
