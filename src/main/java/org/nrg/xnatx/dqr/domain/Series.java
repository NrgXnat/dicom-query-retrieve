/*
 * Series
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.domain;

import java.io.Serializable;
import lombok.Builder;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NonNull;

@Data
@Builder
@EqualsAndHashCode
public class Series implements DqrDomainObject, Serializable {
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
    private final String  seriesInstanceUid;
    private final Study   study;
    private final Integer seriesNumber;
    private final String  modality;
    private final String  seriesDescription;
    private final String  studyDate;
    private final String  studyId;
    private final String  accessionNumber;
    private final String  patientId;
    private final String  patientName;
}
