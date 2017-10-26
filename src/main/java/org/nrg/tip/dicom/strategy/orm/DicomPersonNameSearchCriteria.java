/*
 * org.nrg.tip.dicom.strategy.orm.DicomPersonNameSearchCriteria
 * TIP is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.tip.dicom.strategy.orm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class DicomPersonNameSearchCriteria {

    private List<String> criteriaInOrderOfPreference;

    public DicomPersonNameSearchCriteria() {
        criteriaInOrderOfPreference = new ArrayList<String>(5);
    }

    public List<String> getCriteriaInOrderOfPreference() {
        return new ArrayList<String>(criteriaInOrderOfPreference);
    }

    public void addCriterion(String criterion) {
        criteriaInOrderOfPreference.add(criterion);
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(43, 499).append(criteriaInOrderOfPreference).toHashCode();
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
        DicomPersonNameSearchCriteria other = (DicomPersonNameSearchCriteria) obj;
        return new EqualsBuilder().append(criteriaInOrderOfPreference, other.criteriaInOrderOfPreference).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
