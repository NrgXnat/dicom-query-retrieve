/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.DqrPersonName
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Builder;
import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.dcm4che3.data.PersonName;
import org.dcm4che3.data.PersonName.Component;
import org.dcm4che3.data.PersonName.Group;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class DqrPersonName implements Serializable {
    private static final long serialVersionUID = -4147009089575812390L;

    public DqrPersonName() {
        this(null, null, null, null, null);
    }

    public DqrPersonName(final String firstName, final String lastName) {
        this(firstName, lastName, null, null, null);
    }

    /**
     * Expects one of the following formats: "partialLastName" "fullLastName, partialFirstName" Trims up both components
     * and stuffs them in their respective fields.
     *
     * @param commaDelimitedName The person's last and first name, in that order and separated by a comma
     */
    public DqrPersonName(final String commaDelimitedName) {
        final String[] patientNameParts = StringUtils.trimToEmpty(commaDelimitedName).split("\\s*,\\s*");
        String lastName = StringUtils.trimToNull((patientNameParts.length >= 1 ? patientNameParts[0] : null));
        String firstName = StringUtils.trimToNull((patientNameParts.length >= 2 ? patientNameParts[1] : null));
        personName = buildPersonName(firstName, lastName, null, null, null);
    }

    public DqrPersonName(final PersonName personName) {
        this.personName = personName;
    }

    @Builder
    public DqrPersonName(final String firstName, final String lastName, final String middleName, final String prefix, final String suffix) {
        personName = buildPersonName(firstName, lastName, middleName, prefix, suffix);
    }

    private static PersonName buildPersonName(String firstName, String lastName, String middleName, String prefix, String suffix) {
        // Build DICOM PN format: FamilyName^GivenName^MiddleName^Prefix^Suffix
        StringBuilder sb = new StringBuilder();
        sb.append(StringUtils.defaultString(lastName));
        sb.append("^");
        sb.append(StringUtils.defaultString(firstName));
        sb.append("^");
        sb.append(StringUtils.defaultString(middleName));
        sb.append("^");
        sb.append(StringUtils.defaultString(prefix));
        sb.append("^");
        sb.append(StringUtils.defaultString(suffix));
        return new PersonName(sb.toString(), true);
    }

    public boolean isBlank() {
        return StringUtils.isAllBlank(getLastName(), getFirstName(), getMiddleName(), getPrefix(), getSuffix());
    }

    public boolean hasFirstName() {
        return !StringUtils.isBlank(getFirstName());
    }

    public boolean hasLastName() {
        return !StringUtils.isBlank(getLastName());
    }

    public String getFirstName() {
        return personName.get(Group.Alphabetic, Component.GivenName);
    }

    public String getLastName() {
        return personName.get(Group.Alphabetic, Component.FamilyName);
    }

    public String getMiddleName() {
        return personName.get(Group.Alphabetic, Component.MiddleName);
    }

    public String getPrefix() {
        return personName.get(Group.Alphabetic, Component.NamePrefix);
    }

    public String getSuffix() {
        return personName.get(Group.Alphabetic, Component.NameSuffix);
    }

    @JsonIgnore
    public String getLastNameCommaFirstName() {
        return StringUtils.isBlank(getFirstName()) ? getLastName() : StringUtils.joinWith(", ", getLastName(), getFirstName());
    }

    /**
     * Since the dcm4che PersonName is not Serializable and can't be changed, we need to roll our own serialization
     */
    private void writeObject(final ObjectOutputStream outputStream) throws IOException {
        outputStream.defaultWriteObject();
        outputStream.writeObject(getFirstName());
        outputStream.writeObject(getLastName());
        outputStream.writeObject(getMiddleName());
        outputStream.writeObject(getPrefix());
        outputStream.writeObject(getSuffix());
    }

    private void readObject(final ObjectInputStream inputStream) throws ClassNotFoundException, IOException {
        inputStream.defaultReadObject();
        personName = buildPersonName(
                (String) inputStream.readObject(),
                (String) inputStream.readObject(),
                (String) inputStream.readObject(),
                (String) inputStream.readObject(),
                (String) inputStream.readObject());
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(359, 367)
                .append(getFirstName())
                .append(getLastName())
                .append(getMiddleName())
                .append(getPrefix())
                .append(getSuffix())
                .toHashCode();
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (obj == this) {
            return true;
        }
        if (obj.getClass() != getClass()) {
            return false;
        }
        final DqrPersonName other = (DqrPersonName) obj;
        return new EqualsBuilder().append(getFirstName(), other.getFirstName())
                                  .append(getLastName(), other.getLastName())
                                  .append(getMiddleName(), other.getMiddleName())
                                  .append(getPrefix(), other.getPrefix())
                                  .append(getSuffix(), other.getSuffix())
                                  .isEquals();
    }

    @Override
    public String toString() {
        return personName.toString();
    }

    /**
     * It really does need to be serialized, but we'll roll our own serialization methods that handle this field
     */
    private transient PersonName personName;
}
