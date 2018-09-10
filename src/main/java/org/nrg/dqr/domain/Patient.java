/*
 * Patient
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
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.DateSerializer;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import org.apache.commons.lang.builder.EqualsBuilder;
import org.apache.commons.lang.builder.HashCodeBuilder;
import org.apache.commons.lang.builder.ToStringBuilder;
import org.nrg.dqr.restlet.JsonViews;
import org.nrg.xnat.utils.DateAsStringSerializer;

public class Patient implements DqrDomainObject, Serializable {

    private static final long serialVersionUID = 1L;

    private String id;

    private PatientName name;

    @JsonSerialize(using=DateAsStringSerializer.class)
    private Date birthDate;

    private String sex;

    private Map<String, Study> studies;

    public Patient() {
        studies = new HashMap<String, Study>(10);
    }

    public String getId() {
        return id;
    }

    public void setId(final String id) {
        this.id = id;
    }

    public PatientName getName() {
        return name;
    }

    public void setName(final PatientName name) {
        this.name = name;
    }

    public Date getBirthDate() {
        return birthDate;
    }

    public void setBirthDate(Date birthDate) {
        this.birthDate = birthDate;
    }

    public String getSex() {
        return sex;
    }

    public void setSex(String sex) {
        this.sex = sex;
    }

    @JsonView(JsonViews.PatientRootView.class)
    public Map<String, Study> getStudies() {
        return new HashMap<String, Study>(studies);
    }

    public void setStudies(Map<String, Study> studies) {
        this.studies.clear();
        this.studies.putAll(studies);
    }

    public void addStudy(Study study) {
        studies.put(study.getStudyInstanceUid(), study);
    }

    @Override
    public String getUniqueIdentifier() {
        return getId();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 97).append(id).append(name).append(birthDate).append(sex).append(studies)
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
        Patient other = (Patient) obj;
        return new EqualsBuilder().append(id, other.id).append(name, other.name).append(birthDate, other.birthDate)
                .append(sex, other.sex).append(studies, other.studies).isEquals();
    }

    @Override
    public String toString() {
        return ToStringBuilder.reflectionToString(this);
    }
}
