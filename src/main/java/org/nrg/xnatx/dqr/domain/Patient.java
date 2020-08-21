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
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Patient implements DqrDomainObject, Serializable {
    public Patient(final DicomObject dicomObject, final OrmStrategy ormStrategy) {
        id = StringUtils.trim(dicomObject.getString(Tag.PatientID));
        name = ormStrategy.getPatientNameStrategy().dicomPatientNameToDqrPatientName(StringUtils.trim(dicomObject.getString(Tag.PatientName)));
        sex = StringUtils.trim(dicomObject.getString(Tag.PatientSex));
        if (!StringUtils.isBlank(dicomObject.getString(Tag.PatientBirthDate))) {
            birthDate = dicomObject.getDate(Tag.PatientBirthDate);
        }
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
            properties.add("birthDate: " + DqrDateRange.format(birthDate));
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
