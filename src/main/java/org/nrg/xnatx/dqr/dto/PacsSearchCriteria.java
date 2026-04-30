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
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.apache.commons.lang3.tuple.Pair;
import org.dcm4che3.data.Tag;
import org.nrg.xnatx.dqr.dicom.converters.PacsSearchCriteriaDeserializer;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.Serializable;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static org.nrg.xnatx.dqr.domain.Series.SERIES_NUMBER_PLACEHOLDER;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonDeserialize(using = PacsSearchCriteriaDeserializer.class)
public class PacsSearchCriteria implements Serializable {
    private static final long serialVersionUID = -4480914700711320053L;

    @JsonIgnore
    public boolean isAtLeastOneKeyCriterionSpecified() {
        return !StringUtils.isAllBlank(getPatientId(), getStudyInstanceUid(), getSeriesInstanceUid(), getAccessionNumber());
    }

    private Stream<Pair<Integer, String>> getKeyPairs() {
        Stream<Pair<Integer, String>> raw = Stream.of(
                Pair.of(Tag.PatientID, patientId),
                Pair.of(Tag.PatientName, patientName),
                Pair.of(Tag.PatientBirthDate, dob),
                Pair.of(Tag.AccessionNumber, accessionNumber),
                Pair.of(Tag.StudyInstanceUID, studyInstanceUid),
                Pair.of(Tag.StudyID, studyId),
                Pair.of(Tag.SeriesInstanceUID, seriesInstanceUid),
                Pair.of(Tag.SeriesDescription, seriesDescription),
                Pair.of(Tag.ModalitiesInStudy, modality)
        );
        if (seriesNumber > SERIES_NUMBER_PLACEHOLDER) {
            raw = Stream.concat(raw, Stream.of(Pair.of(Tag.SeriesNumber, Integer.toString(seriesNumber))));
        }
        return raw.filter(p -> StringUtils.isNotBlank(p.getValue()));
    }

    @JsonIgnore
    public List<Pair<int[], String>> getDicomKeys() {
        return getKeyPairs()
                .map(p -> Pair.of(new int[] {p.getKey()}, p.getValue()))
                .collect(Collectors.toList());
    }

    @JsonIgnore
    public Map<Integer, String> getDicomKeysMap() {
        return getKeyPairs().collect(Collectors.toMap(Pair::getKey, Pair::getValue));
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }

    private long         pacsId;
    private String       patientId;
    private String       patientName;
    private String       dob;
    private String       accessionNumber;
    private String       studyInstanceUid;
    private String       studyId;
    private String       seriesInstanceUid;
    private String       seriesDescription;
    private int          seriesNumber;
    private String       modality;
    private DqrDateRange studyDateRange;
}
