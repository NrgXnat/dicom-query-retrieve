/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.Series
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain;

import lombok.Builder;
import lombok.EqualsAndHashCode;
import lombok.NonNull;
import lombok.Value;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;

import java.io.Serializable;

@Value
@Builder
@EqualsAndHashCode
public class Series implements DqrDomainObject, Serializable {
    private static final long serialVersionUID = 8880479205274075901L;

    public static int SERIES_NUMBER_PLACEHOLDER = 0;

    @Override
    public String getUniqueIdentifier() {
        return getSeriesInstanceUid();
    }

    /**
     * Create a Series from the given DICOM attributes.
     *
     * @param attributes the DICOM attributes
     * @return the Series
     */
    public static Series from(final Attributes attributes) {
        final Series.SeriesBuilder builder = builder()
                .study(new Study(StringUtils.trim(attributes.getString(Tag.StudyInstanceUID))))
                .seriesInstanceUid(StringUtils.trim(attributes.getString(Tag.SeriesInstanceUID)))
                .modality(StringUtils.trim(attributes.getString(Tag.Modality)))
                .seriesDescription(StringUtils.trim(attributes.getString(Tag.SeriesDescription)))
                .studyDate(StringUtils.trim(attributes.getString(Tag.StudyDate)))
                .studyId(StringUtils.trim(attributes.getString(Tag.StudyID)))
                .accessionNumber(StringUtils.trim(attributes.getString(Tag.AccessionNumber)))
                .patientId(StringUtils.trim(attributes.getString(Tag.PatientID)))
                .patientName(StringUtils.trim(attributes.getString(Tag.PatientName)));

        if (StringUtils.isNotBlank(attributes.getString(Tag.SeriesNumber))) {
            builder.seriesNumber(attributes.getInt(Tag.SeriesNumber, SERIES_NUMBER_PLACEHOLDER));
        }

        return builder.build();
    }

    @Override
    public String toString() {
        return "{ study: " + study + ", " +
               "seriesInstanceUid: " + seriesInstanceUid + ", " +
               "seriesNumber: " + seriesNumber + ", " +
               "modality: " + modality + ", " +
               "seriesDescription: " + seriesDescription + ", " +
               "studyDate: " + studyDate + ", " +
               "studyId: " + studyId + ", " +
               "accessionNumber: " + accessionNumber + ", " +
               "patientId: " + patientId + ", " +
               "patientName: " + patientName + "}";
    }

    @NonNull
    String seriesInstanceUid;
    Study   study;
    Integer seriesNumber;
    String  modality;
    String  seriesDescription;
    String  studyDate;
    String  studyId;
    String  accessionNumber;
    String  patientId;
    String  patientName;
}
