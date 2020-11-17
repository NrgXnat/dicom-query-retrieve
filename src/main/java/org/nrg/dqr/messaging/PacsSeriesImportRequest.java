/*
 * PacsSeriesImportRequest
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

import java.io.Serializable;

import lombok.Builder;
import lombok.Getter;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nrg.dqr.domain.Study;
import org.nrg.dqr.domain.Series;

@Getter
@Builder
public class PacsSeriesImportRequest implements Serializable {
    @Override
    public int hashCode() {
        return new HashCodeBuilder(97, 503).append(series).toHashCode();
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
        return new EqualsBuilder().append(series, other.series).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    private final Series series;
    private final Study  study;
}
