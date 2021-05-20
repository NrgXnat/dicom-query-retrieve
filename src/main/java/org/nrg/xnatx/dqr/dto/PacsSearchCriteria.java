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
import org.apache.commons.lang3.tuple.Pair;
import org.dcm4che2.data.Tag;
import org.nrg.xnatx.dqr.dicom.converters.PacsSearchCriteriaDeserializer;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

@Value
@Builder
@JsonDeserialize(using = PacsSearchCriteriaDeserializer.class)
public class PacsSearchCriteria implements Serializable {
    private static final long serialVersionUID = -4480914700711320053L;

    @JsonIgnore
    public boolean isAtLeastOneKeyCriterionSpecified() {
        return !StringUtils.isAllBlank(getPatientId(), getStudyInstanceUid(), getSeriesInstanceUid(), getAccessionNumber());
    }

    @JsonIgnore
    public List<Pair<int[], String>> getDicomKeys() {
        final List<Pair<int[], String>> keys = new ArrayList<>();
        getKey(keys, Tag.PatientID, patientId);
        getKey(keys, Tag.PatientName, patientName);
        getKey(keys, Tag.PatientBirthDate, dob);
        getKey(keys, Tag.AccessionNumber, accessionNumber);
        getKey(keys, Tag.StudyInstanceUID, studyInstanceUid);
        getKey(keys, Tag.StudyID, studyId);
        getKey(keys, Tag.SeriesInstanceUID, seriesInstanceUid);
        getKey(keys, Tag.SeriesDescription, seriesDescription);
        if (seriesNumber > 0) {
            getKey(keys, Tag.SeriesNumber, Integer.toString(seriesNumber));
        }
        getKey(keys, Tag.ModalitiesInStudy, modality);
        return keys;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    private void getKey(final List<Pair<int[], String>> keys, final int tag, final String value) {
        if (StringUtils.isBlank(value)) {
            return;
        }
        keys.add(Pair.of(new int[] {tag}, value));
    }

    long         pacsId;
    String       patientId;
    String       patientName;
    String       dob;
    String       accessionNumber;
    String       studyInstanceUid;
    String       studyId;
    String       seriesInstanceUid;
    String       seriesDescription;
    int          seriesNumber;
    String       modality;
    DqrDateRange studyDateRange;
}
