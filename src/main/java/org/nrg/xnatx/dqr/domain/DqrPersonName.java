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
import org.dcm4che2.data.PersonName;

import java.io.IOException;
import java.io.ObjectInputStream;
import java.io.ObjectOutputStream;
import java.io.Serializable;

public class DqrPersonName implements Serializable {
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
        personName = new PersonName();
        personName.set(PersonName.FAMILY, StringUtils.trimToNull((patientNameParts.length >= 1 ? patientNameParts[0] : null)));
        personName.set(PersonName.GIVEN, StringUtils.trimToNull((patientNameParts.length >= 2 ? patientNameParts[1] : null)));
    }

    public DqrPersonName(final PersonName personName) {
        this.personName = personName;
    }

    @Builder
    public DqrPersonName(final String firstName, final String lastName, final String middleName, final String prefix, final String suffix) {
        personName = new PersonName();
        setPersonNameFields(firstName, lastName, middleName, prefix, suffix);
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
        return personName.get(PersonName.GIVEN);
    }

    public String getLastName() {
        return personName.get(PersonName.FAMILY);
    }

    public String getMiddleName() {
        return personName.get(PersonName.MIDDLE);
    }

    public String getPrefix() {
        return personName.get(PersonName.PREFIX);
    }

    public String getSuffix() {
        return personName.get(PersonName.SUFFIX);
    }

    @JsonIgnore
    public String getLastNameCommaFirstName() {
        return StringUtils.isBlank(getFirstName()) ? getLastName() : StringUtils.joinWith(", ", getLastName(), getFirstName());
    }

    private void setPersonNameFields(final String firstName, final String lastName, final String middleName,
                                     final String prefix, final String suffix) {
        personName.set(PersonName.FAMILY, lastName);
        personName.set(PersonName.GIVEN, firstName);
        personName.set(PersonName.MIDDLE, middleName);
        personName.set(PersonName.PREFIX, prefix);
        personName.set(PersonName.SUFFIX, suffix);
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
        personName = new PersonName();
        setPersonNameFields((String) inputStream.readObject(),
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
