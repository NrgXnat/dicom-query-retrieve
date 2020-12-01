/*
 * PacsSessionExportRequest
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

import lombok.Builder;
import lombok.Getter;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nrg.dqr.domain.entities.Pacs;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

@Getter
@Builder
public class PacsSessionExportRequest implements Serializable {
    @Override
    public int hashCode() {
        return new HashCodeBuilder(89, 167).append(pacs).append(dateRequested).append(username).toHashCode();
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
        final PacsSessionExportRequest other = (PacsSessionExportRequest) obj;
        return new EqualsBuilder().append(pacs, other.pacs).append(dateRequested, other.dateRequested)
                                  .append(username, other.username).isEquals();
    }

    @Override
    public String toString() {
        return new ToStringBuilder(this).append(pacs).append(StringUtils.defaultIfBlank(sessionId, "null"))
                                        .append(dateRequested).append(StringUtils.defaultIfBlank(username, "null"))
                                        .append(scans).toString();
    }

    private final String                      username;
    private final Pacs                        pacs;
    private final String                      sessionId;
    @Builder.Default
    private final Date                        dateRequested = new Date();
    @Singular
    private final List<PacsScanExportRequest> scans;
}
