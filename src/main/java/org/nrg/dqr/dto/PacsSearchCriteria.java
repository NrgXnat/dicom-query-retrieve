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

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ToStringBuilder;
import org.nrg.xnat.utils.DateRange;
import org.nrg.xnat.utils.DqrDateRange;

public class PacsSearchCriteria {

    private String patientId;

    private String patientName;

    private String studyInstanceUid;

    private String seriesInstanceUid;

    private String accessionNumber;

    private DqrDateRange studyDateRange;

    private String modality;

    private String dob;

    public String getPatientId() {
        return patientId;
    }

    public void setPatientId(String patientId) {
        this.patientId = patientId;
    }

    public String getPatientName() {
        return patientName;
    }

    /**
     * expected format: last[, first]
     *
     * @return
     */
    public void setPatientName(String patientName) {
        this.patientName = patientName;
    }

    public String getStudyInstanceUid() {
        return studyInstanceUid;
    }

    public void setStudyInstanceUid(String studyInstanceUid) {
        this.studyInstanceUid = studyInstanceUid;
    }

    public String getSeriesInstanceUid() {
        return seriesInstanceUid;
    }

    public void setSeriesInstanceUid(String seriesInstanceUid) {
        this.seriesInstanceUid = seriesInstanceUid;
    }

    public String getAccessionNumber() {
        return accessionNumber;
    }

    public void setAccessionNumber(String accessionNumber) {
        this.accessionNumber = accessionNumber;
    }

    public DqrDateRange getStudyDateRange() {
        return studyDateRange;
    }

    public void setStudyDateRange(DqrDateRange studyDateRange) {
        this.studyDateRange = studyDateRange;
    }

    public String getModality() {
        return modality;
    }

    public void setModality(String modality) {
        this.modality = modality;
    }

    public String getDob() {
        return dob;
    }

    public void setDob(String dob) {
        this.dob = dob;
    }

    public boolean isFirstNamePresent() {
        return StringUtils.trimToEmpty(getPatientName()).matches(".+,\\s*\\S+");
    }

    public boolean isFirstNamePartial() {
        return true;
    }

    public boolean isLastNamePartial() {
        return !isFirstNamePresent();
    }

    public boolean isAtLeastOneKeyCriterionSpecified() {
        return !StringUtils.isBlank(getPatientId()) || !StringUtils.isBlank(getStudyInstanceUid())
                || !StringUtils.isBlank(getSeriesInstanceUid()) || !StringUtils.isBlank(getAccessionNumber());
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
