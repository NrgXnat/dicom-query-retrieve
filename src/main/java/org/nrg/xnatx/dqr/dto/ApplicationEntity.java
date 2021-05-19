/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.ApplicationEntity
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import lombok.Builder;
import lombok.Data;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nrg.xnatx.dqr.utils.AeTitle;

@Data
@Builder
public class ApplicationEntity implements Comparable<ApplicationEntity> {
    private AeTitle aeTitle;

    private String label;

    private boolean isDefaultStorageDestination;

    public void setAeTitle(final String aeTitleAndPort) {
        this.aeTitle = new AeTitle(aeTitleAndPort);
    }

    public String getDisplayString() {
        if (!StringUtils.isBlank(label)) {
            return label;
        }
        if (aeTitle != null) {
            return aeTitle.toString();
        }
        return "";
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int compareTo(final ApplicationEntity other) {
        return getDisplayString().compareTo(other.getDisplayString());
    }
}
