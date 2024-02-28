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
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che3.data.Attributes;
import org.dcm4che3.data.PersonName;
import org.dcm4che3.data.Tag;
import org.nrg.xnatx.dqr.utils.DqrDateRange;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.function.Function;

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

    /**
     * Create a Study from the given DICOM attributes.
     * <p>
     * Replicates logic from
     * {@link org.nrg.xnatx.dqr.dicom.command.cfind.dcm4che.tool.CFindSCUStudyLevel#mapDicomObjectToDomainObject(org.dcm4che2.data.DicomObject)}
     *
     * @param attributes the DICOM attributes
     * @param patientNamer the function to use to create a DqrPersonName from a DICOM person name
     * @return the Series
     */
    public static Study from(final Attributes attributes, final Function<String, DqrPersonName> patientNamer) {
        final StudyBuilder builder = Study.builder()
                .studyInstanceUid(attributes.getString(Tag.StudyInstanceUID))
                .studyId(attributes.getString(Tag.StudyID))
                .accessionNumber(attributes.getString(Tag.AccessionNumber))
                .studyDescription(StringUtils.trim(attributes.getString(Tag.StudyDescription)))
                .patient(Patient.from(attributes, patientNamer));
        if (attributes.containsValue(Tag.StudyDate) && !StringUtils.isBlank(attributes.getString(Tag.StudyDate))) {
            builder.studyDate(attributes.getDate(Tag.StudyDate));
        }
        if (attributes.containsValue(Tag.ReferringPhysicianName)) {
            builder.referringPhysicianName(new ReferringPhysicianName(new PersonName(attributes.getString(Tag.ReferringPhysicianName))));
        }
        if (attributes.containsValue(Tag.ModalitiesInStudy)) {
            builder.modalitiesInStudy(Arrays.asList(attributes.getStrings(Tag.ModalitiesInStudy)));
        }
        return builder.build();
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
