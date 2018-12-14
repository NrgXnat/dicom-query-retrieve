/*
 * Study
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
import java.text.SimpleDateFormat;
import java.util.Date;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.nrg.dqr.restlet.JsonViews;
import org.nrg.xnat.utils.DateAsStringSerializer;

public class Study implements DqrDomainObject, Serializable {

    private static final long serialVersionUID = 1L;

    private Patient patient;

    private String studyInstanceUid;

    private Date studyDate;

    private ReferringPhysicianName referringPhysicianName;

    private String projectId;

    private String studyId;

    private String accessionNumber;

    private String studyDescription;

    private String[] modalitiesInStudy;

    public Study() {
    }

    public Study(String studyInstanceUid) {
        setProjectId("");
        setStudyInstanceUid(studyInstanceUid);
    }

    public Study(String projectId, String studyInstanceUid) {
        setProjectId(projectId);
        setStudyInstanceUid(studyInstanceUid);
    }

    @JsonView(JsonViews.StudyRootView.class)
    public Patient getPatient() {
        return patient;
    }

    public void setPatient(final Patient patient) {
        this.patient = patient;
    }

    public String getStudyInstanceUid() {
        return studyInstanceUid;
    }

    public void setStudyInstanceUid(String studyInstanceUid) {
        this.studyInstanceUid = studyInstanceUid;
    }

    @JsonSerialize(using=DateAsStringSerializer.class)
    public Date getStudyDate() {
        return studyDate;
    }

    public String getStudyDateFormattedAs(String format) {
        return null == studyDate ? "" : new SimpleDateFormat(format).format(studyDate);
    }

    public void setStudyDate(Date studyDate) {
        this.studyDate = studyDate;
    }

    public ReferringPhysicianName getReferringPhysicianName() {
        return referringPhysicianName;
    }

    public void setReferringPhysicianName(ReferringPhysicianName referringPhysicianName) {
        this.referringPhysicianName = referringPhysicianName;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
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

    public String getStudyDescription() {
        return studyDescription;
    }

    public void setStudyDescription(String studyDescription) {
        this.studyDescription = studyDescription;
    }

    public String[] getModalitiesInStudy() {
        return modalitiesInStudy;
    }

    public void setModalitiesInStudy(String[] modalitiesInStudy) {
        this.modalitiesInStudy = modalitiesInStudy;
    }

    @Override
    public String getUniqueIdentifier() {
        return getStudyInstanceUid();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(59, 419).append(patient).append(studyInstanceUid).append(studyDate)
                .append(referringPhysicianName).append(studyId).append(accessionNumber).append(studyDescription).append(modalitiesInStudy)
                .toHashCode();
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
        Study other = (Study) obj;
        return new EqualsBuilder().append(patient, other.patient).append(studyInstanceUid, other.studyInstanceUid)
                .append(studyDate, other.studyDate).append(referringPhysicianName, other.referringPhysicianName)
                .append(studyId, other.studyId).append(accessionNumber, other.accessionNumber)
                .append(studyDescription, other.studyDescription).append(modalitiesInStudy, other.modalitiesInStudy).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
