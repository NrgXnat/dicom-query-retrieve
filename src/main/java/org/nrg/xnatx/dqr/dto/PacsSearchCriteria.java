/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dto.PacsSearchCriteria
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import lombok.Builder;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nrg.xnatx.dqr.dicom.converters.PacsSearchCriteriaDeserializer;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.Serializable;

@Value
@Builder
@JsonDeserialize(using = PacsSearchCriteriaDeserializer.class)
public class PacsSearchCriteria implements Serializable {
    private static final long serialVersionUID = -4480914700711320053L;

    @JsonIgnore
    public boolean isAtLeastOneKeyCriterionSpecified() {
        return !StringUtils.isAllBlank(getPatientId(), getStudyInstanceUid(), getSeriesInstanceUid(), getAccessionNumber());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    long         pacsId;
    String       patientId;
    String       patientName;
    String       studyInstanceUid;
    String       seriesInstanceUid;
    String       accessionNumber;
    String       modality;
    String       dob;
    DqrDateRange studyDateRange;
}
