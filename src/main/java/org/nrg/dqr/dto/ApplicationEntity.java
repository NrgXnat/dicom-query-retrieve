/*
 * PacsSearchCriteria
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.dqr.dto;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.apache.commons.lang3.builder.CompareToBuilder;
import org.nrg.xdat.om.base.BaseXnatPvisitdata;

import javax.validation.constraints.NotNull;

public class ApplicationEntity implements Comparable<ApplicationEntity>{

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
        if(!StringUtils.isBlank(label)){
            return label;
        }
        else if(!StringUtils.isBlank(aeTitle)){
            return aeTitle;
        }
        else{
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
