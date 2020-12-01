/*
 * PacsStudyImportRequest
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.messaging;

import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.entities.Pacs;
import org.nrg.xft.security.UserI;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

public class PacsStudyImportRequest implements Serializable {
    private Pacs pacs;

    private Study study;

    private Date dateRequested;

    private UserI requestingUser;

    private List<PacsSeriesImportRequest> series;

    public PacsStudyImportRequest() {
        series = new ArrayList<>();
    }

    public Pacs getPacs() {
        return pacs;
    }

    public void setPacs(Pacs pacs) {
        this.pacs = pacs;
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(Study study) {
        this.study = study;
    }

    @SuppressWarnings("unused")
    public Date getDateRequested() {
        return dateRequested;
    }

    public void setDateRequested(Date dateRequested) {
        this.dateRequested = dateRequested;
    }

    public UserI getRequestingUser() {
        return requestingUser;
    }

    public void setRequestingUser(UserI requestingUser) {
        this.requestingUser = requestingUser;
    }

    public List<PacsSeriesImportRequest> getSeries() {
        return series;
    }

    public void setSeries(List<PacsSeriesImportRequest> series) {
        this.series = series;
    }

    public void addSeries(PacsSeriesImportRequest pacsSeriesImportRequest) {
        series.add(pacsSeriesImportRequest);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(89, 167).append(pacs).append(study).append(dateRequested).append(requestingUser)
                                           .append(series).toHashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        PacsStudyImportRequest other = (PacsStudyImportRequest) obj;
        return new EqualsBuilder().append(pacs, other.pacs).append(study, other.study)
                                  .append(dateRequested, other.dateRequested).append(requestingUser, other.requestingUser)
                                  .append(series, other.series).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
