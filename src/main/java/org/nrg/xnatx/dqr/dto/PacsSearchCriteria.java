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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nrg.xnatx.dqr.dicom.converters.PacsSearchCriteriaDeserializer;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

@Data
@Accessors(prefix = "_")
@Builder
@AllArgsConstructor
@JsonDeserialize(using = PacsSearchCriteriaDeserializer.class)
public class PacsSearchCriteria {
    @JsonIgnore
    public boolean isAtLeastOneKeyCriterionSpecified() {
        return !StringUtils.isAllBlank(getPatientId(), getStudyInstanceUid(), getSeriesInstanceUid(), getAccessionNumber());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    private final long         _pacsId;
    private final String       _patientId;
    private final String       _patientName;
    private final String       _studyInstanceUid;
    private final String       _seriesInstanceUid;
    private final String       _accessionNumber;
    private final String       _modality;
    private final String       _dob;
    private final DqrDateRange _studyDateRange;
}
