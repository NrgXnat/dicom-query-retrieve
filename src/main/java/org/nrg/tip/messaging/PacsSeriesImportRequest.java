/*
 * org.nrg.tip.messaging.PacsSeriesImportRequest
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.messaging;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.nrg.tip.domain.Series;
import org.nrg.tip.domain.Study;

public class PacsSeriesImportRequest implements Serializable {

    private static final long serialVersionUID = 1L;

    private Series _series;
    private Study _study;

    public Series getSeries() {
        return _series;
    }

    public void setSeries(final Series series) {
        _series = series;
    }

    public Study getStudy() {
        return _study;
    }

    public void setStudy(final Study study) {
        _study = study;
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(97, 503).append(_series).toHashCode();
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
        final PacsSeriesImportRequest other = (PacsSeriesImportRequest) obj;
        return new EqualsBuilder().append(_series, other._series).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
