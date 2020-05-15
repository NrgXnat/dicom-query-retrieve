/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.ApplicationEntity
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;

public class ApplicationEntity implements Comparable<ApplicationEntity> {

    private String aeTitle;

    private String label;

    private boolean isDefaultStorageDestination;

    public String getAeTitle() {
        return aeTitle;
    }

    public void setAeTitle(String aeTitle) {
        this.aeTitle = aeTitle;
    }

    public String getLabel() {
        return label;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public boolean isDefaultStorageDestination() {
        return isDefaultStorageDestination;
    }

    public void setIsDefaultStorageDestination(boolean isDefaultStorageDestination) {
        this.isDefaultStorageDestination = isDefaultStorageDestination;
    }

    public String getDisplayString() {
        if (!StringUtils.isBlank(label)) {
            return label;
        } else if (!StringUtils.isBlank(aeTitle)) {
            return aeTitle;
        } else {
            return "";
        }
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    @Override
    public int compareTo(final ApplicationEntity other) {
        return this.getDisplayString().compareTo(other.getDisplayString());
    }
}
