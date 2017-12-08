/*
 * Series
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.domain;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Series implements DqrDomainObject, Serializable {

    private static final long serialVersionUID = 1L;

    private Study study;

    private String seriesInstanceUid;

    private Integer seriesNumber;

    private String modality;

    private String seriesDescription;

    public Series() {
    }

    public Series(String seriesInstanceUid) {
        setSeriesInstanceUid(seriesInstanceUid);
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(final Study study) {
        this.study = study;
    }

    public String getSeriesInstanceUid() {
        return seriesInstanceUid;
    }

    public void setSeriesInstanceUid(final String seriesInstanceUid) {
        this.seriesInstanceUid = seriesInstanceUid;
    }

    public Integer getSeriesNumber() {
        return seriesNumber;
    }

    public void setSeriesNumber(final Integer seriesNumber) {
        this.seriesNumber = seriesNumber;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(final String modality) {
        this.modality = modality;
    }

    public String getSeriesDescription() {
        return seriesDescription;
    }

    public void setSeriesDescription(String seriesDescription) {
        this.seriesDescription = seriesDescription;
    }

    @Override
    public String getUniqueIdentifier() {
        return getSeriesInstanceUid();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(61, 67).append(study).append(seriesInstanceUid).append(seriesNumber)
                .append(modality).append(seriesDescription).toHashCode();
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
        Series other = (Series) obj;
        return new EqualsBuilder().append(study, other.study).append(seriesInstanceUid, other.seriesInstanceUid)
                .append(seriesNumber, other.seriesNumber).append(modality, other.modality)
                .append(seriesDescription, other.seriesDescription).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
