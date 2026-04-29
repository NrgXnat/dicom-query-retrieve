/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.Patient
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain;

import com.fasterxml.jackson.annotation.JsonBackReference;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.Tag;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Patient implements DqrDomainObject, Serializable {
    private static final long serialVersionUID = -3085053888171875651L;

    /**
     * Create a Patient from the given DICOM attributes.
     *
     * @param attributes the DICOM attributes
     * @param patientNamer the function to create a DqrPersonName from the PatientName string
     * @return the Patient
     */
    public static Patient from(final Attributes attributes, final Function<String, DqrPersonName> patientNamer) {
        final Patient.PatientBuilder builder = Patient.builder()
                .id(StringUtils.trim(attributes.getString(Tag.PatientID)))
                .name(patientNamer.apply(StringUtils.trim(attributes.getString(Tag.PatientName))))
                .sex(StringUtils.trim(attributes.getString(Tag.PatientSex)));
        if (!StringUtils.isBlank(attributes.getString(Tag.PatientBirthDate))) {
            builder.birthDate(attributes.getDate(Tag.PatientBirthDate));
        }
        return builder.build();
    }

    @Override
    public String toString() {
        final List<String> properties = new ArrayList<>();
        if (StringUtils.isNotBlank(id)) {
            properties.add("id: " + id);
        }
        if (name != null && !name.isBlank()) {
            properties.add("name: " + name.getLastNameCommaFirstName());
        }
        if (birthDate != null) {
            properties.add("birthDate: " + DqrDateRange.formatDate(birthDate));
        }
        if (StringUtils.isNotBlank(sex)) {
            properties.add("sex: " + sex);
        }
        if (studies != null && !studies.isEmpty()) {
            properties.add("studies: { " + StringUtils.join(studies, ", ") + " }");
        }
        return "{ " + StringUtils.join(properties, ", ") + " }";
    }

    @Override
    public String getUniqueIdentifier() {
        return getId();
    }

    private String id;

    private DqrPersonName name;

    private Date birthDate;

    private String sex;

    @JsonBackReference
    @Singular
    private Collection<Study> studies;
}
