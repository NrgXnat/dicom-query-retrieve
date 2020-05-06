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

package org.nrg.xnatx.dqr.domain;

import com.fasterxml.jackson.annotation.JsonView;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import java.io.Serializable;
import java.util.Date;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import org.nrg.xnatx.dqr.restlet.JsonViews;
import org.nrg.xnatx.dqr.utils.DateAsStringSerializer;

@Data
@Builder
@AllArgsConstructor
public class Study implements DqrDomainObject, Serializable {
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

    @Override
    public String getUniqueIdentifier() {
        return getStudyInstanceUid();
    }

    @JsonView(JsonViews.StudyRootView.class)
    private Patient                patient;
    private String                 studyInstanceUid;
    @JsonSerialize(using = DateAsStringSerializer.class)
    private Date                   studyDate;
    private ReferringPhysicianName referringPhysicianName;
    private String                 projectId;
    private String                 studyId;
    private String                 accessionNumber;
    private String                 studyDescription;
    private String[]               modalitiesInStudy;
}
