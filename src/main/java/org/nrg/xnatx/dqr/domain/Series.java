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

import java.io.Serializable;

@Value
@Builder
@EqualsAndHashCode
public class Series implements DqrDomainObject, Serializable {
    private static final long serialVersionUID = 8880479205274075901L;

    @Override
    public String getUniqueIdentifier() {
        return getSeriesInstanceUid();
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
