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

package org.nrg.xnatx.dqr.domain;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.Date;
import java.util.Map;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.xnatx.dqr.dicom.strategy.orm.OrmStrategy;
import org.nrg.xnatx.dqr.restlet.JsonViews;
import org.nrg.xnatx.dqr.utils.DateAsStringSerializer;

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
    public String getUniqueIdentifier() {
        return getId();
    }

    private String id;

    private PatientName name;

    @JsonSerialize(using = DateAsStringSerializer.class)
    private Date birthDate;

    private String sex;

    @JsonView(JsonViews.PatientRootView.class)
    @Singular
    private Map<String, Study> studies;
}
