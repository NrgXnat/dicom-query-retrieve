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

package org.nrg.dqr.domain;

import java.io.Serializable;

import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;

public class Series implements DqrDomainObject, Serializable {

    private static final long serialVersionUID = 1L;

    private Study study;

    private String seriesInstanceUid;

    private Integer seriesNumber;

    private String modality;

    private String seriesDescription;

    private String studyDate;

    private String studyId;

    private String accessionNumber;

    private String patientId;

    private String patientName;

    public Series() {
    }

    public Series(String seriesInstanceUid) {
        setSeriesInstanceUid(seriesInstanceUid);
    }

    public Study getStudy() {
        return study;
    }

    public void setStudy(final Study study) {
        this.study = study;
    }

    public String getSeriesInstanceUid() {
        return seriesInstanceUid;
    }

    public void setSeriesInstanceUid(final String seriesInstanceUid) {
        this.seriesInstanceUid = seriesInstanceUid;
    }

    public Integer getSeriesNumber() {
        return seriesNumber;
    }

    public void setSeriesNumber(final Integer seriesNumber) {
        this.seriesNumber = seriesNumber;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(final String modality) {
        this.modality = modality;
    }

    public String getSeriesDescription() {
        return seriesDescription;
    }

    public void setSeriesDescription(String seriesDescription) {
        this.seriesDescription = seriesDescription;
    }

    public String getStudyDate() {
        return studyDate;
    }

    public void setStudyDate(String studyDate) {
        this.studyDate = studyDate;
    }

    public String getStudyId() {
        return studyId;
    }

    public void setStudyId(String studyId) {
        this.studyId = studyId;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    @Override
    public String getUniqueIdentifier() {
        return getSeriesInstanceUid();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;

        Series series = (Series) o;

        if (study != null ? !study.equals(series.study) : series.study != null) return false;
        if (seriesInstanceUid != null ? !seriesInstanceUid.equals(series.seriesInstanceUid) : series.seriesInstanceUid != null)
            return false;
        if (seriesNumber != null ? !seriesNumber.equals(series.seriesNumber) : series.seriesNumber != null)
            return false;
        if (modality != null ? !modality.equals(series.modality) : series.modality != null) return false;
        if (seriesDescription != null ? !seriesDescription.equals(series.seriesDescription) : series.seriesDescription != null)
            return false;
        if (studyDate != null ? !studyDate.equals(series.studyDate) : series.studyDate != null) return false;
        if (studyId != null ? !studyId.equals(series.studyId) : series.studyId != null) return false;
        if (accessionNumber != null ? !accessionNumber.equals(series.accessionNumber) : series.accessionNumber != null)
            return false;
        if (patientId != null ? !patientId.equals(series.patientId) : series.patientId != null) return false;
        return patientName != null ? patientName.equals(series.patientName) : series.patientName == null;
    }

    @Override
    public int hashCode() {
        int result = study != null ? study.hashCode() : 0;
        result = 31 * result + (seriesInstanceUid != null ? seriesInstanceUid.hashCode() : 0);
        result = 31 * result + (seriesNumber != null ? seriesNumber.hashCode() : 0);
        result = 31 * result + (modality != null ? modality.hashCode() : 0);
        result = 31 * result + (seriesDescription != null ? seriesDescription.hashCode() : 0);
        result = 31 * result + (studyDate != null ? studyDate.hashCode() : 0);
        result = 31 * result + (studyId != null ? studyId.hashCode() : 0);
        result = 31 * result + (accessionNumber != null ? accessionNumber.hashCode() : 0);
        result = 31 * result + (patientId != null ? patientId.hashCode() : 0);
        result = 31 * result + (patientName != null ? patientName.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
