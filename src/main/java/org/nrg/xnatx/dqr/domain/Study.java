/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.Study
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain;

import com.fasterxml.jackson.annotation.JsonManagedReference;
import lombok.*;
import org.apache.commons.lang3.StringUtils;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class Study implements DqrDomainObject, Serializable {
    private static final long serialVersionUID = 8640039199226827418L;

    public Study(final String studyInstanceUid) {
        setProjectId("");
        setStudyInstanceUid(studyInstanceUid);
    }

    public Study(final String projectId, final String studyInstanceUid) {
        setProjectId(projectId);
        setStudyInstanceUid(studyInstanceUid);
    }

    @Override
    public String toString() {
        final List<String> properties = new ArrayList<>();
        if (StringUtils.isNotBlank(studyInstanceUid)) {
            properties.add("studyInstanceUid: " + studyInstanceUid);
        }
        if (StringUtils.isNotBlank(studyId)) {
            properties.add("studyId: " + studyId);
        }
        if (StringUtils.isNotBlank(studyDescription)) {
            properties.add("studyDescription: " + studyDescription);
        }
        if (StringUtils.isNotBlank(projectId)) {
            properties.add("projectId: " + projectId);
        }
        if (StringUtils.isNotBlank(accessionNumber)) {
            properties.add("accessionNumber: " + accessionNumber);
        }
        if (studyDate != null) {
            properties.add("studyDate: " + DqrDateRange.formatDate(studyDate));
        }
        if (patient != null) {
            properties.add("patient: " + patient);
        }
        if (referringPhysicianName != null) {
            properties.add("referringPhysicianName: " + referringPhysicianName);
        }
        if (modalitiesInStudy != null && !modalitiesInStudy.isEmpty()) {
            properties.add("modalitiesInStudy: [" + StringUtils.join(modalitiesInStudy, ", ") + "]");
        }
        return "{ " + StringUtils.join(properties, ", ") + " }";
    }

    @Override
    public String getUniqueIdentifier() {
        return getStudyInstanceUid();
    }

    public String getFormattedStudyDate() {
        return DqrDateRange.formatDate(studyDate);
    }

    @JsonManagedReference
    private Patient                patient;
    private String                 studyInstanceUid;
    private Date                   studyDate;
    private ReferringPhysicianName referringPhysicianName;
    private String                 projectId;
    private String                 studyId;
    private String                 accessionNumber;
    private String                 studyDescription;
    @Singular("modalityInStudy")
    private List<String>           modalitiesInStudy;
}
