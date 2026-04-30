/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.dcm4che3.QueryRetrieveLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.dcm4che3;

import lombok.Getter;
import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;

import java.util.List;

/**
 * Query/Retrieve levels for DICOM C-FIND and C-MOVE operations.
 */
@Getter
public enum QueryRetrieveLevel {

    PATIENT("PATIENT",
            UID.PatientRootQueryRetrieveInformationModelFind,
            UID.PatientRootQueryRetrieveInformationModelMove,
            List.of(Tag.PatientID, Tag.PatientName, Tag.PatientBirthDate, Tag.PatientSex)),

    STUDY("STUDY",
            UID.StudyRootQueryRetrieveInformationModelFind,
            UID.StudyRootQueryRetrieveInformationModelMove,
            List.of(Tag.StudyInstanceUID, Tag.StudyID, Tag.StudyDate, Tag.StudyTime,
                    Tag.AccessionNumber, Tag.PatientID, Tag.PatientName)),

    SERIES("SERIES",
            UID.StudyRootQueryRetrieveInformationModelFind,
            UID.StudyRootQueryRetrieveInformationModelMove,
            List.of(Tag.SeriesInstanceUID, Tag.SeriesNumber, Tag.Modality,
                    Tag.SeriesDescription, Tag.StudyInstanceUID)),

    IMAGE("IMAGE",
            UID.StudyRootQueryRetrieveInformationModelFind,
            UID.StudyRootQueryRetrieveInformationModelMove,
            List.of(Tag.SOPInstanceUID, Tag.SOPClassUID, Tag.InstanceNumber,
                    Tag.SeriesInstanceUID));

    private final String levelName;
    private final String findSopClass;
    private final String moveSopClass;
    private final List<Integer> defaultReturnKeys;

    QueryRetrieveLevel(String levelName, String findSopClass, String moveSopClass, List<Integer> defaultReturnKeys) {
        this.levelName = levelName;
        this.findSopClass = findSopClass;
        this.moveSopClass = moveSopClass;
        this.defaultReturnKeys = defaultReturnKeys;
    }

    public static QueryRetrieveLevel fromString(String level) {
        for (QueryRetrieveLevel qrl : values()) {
            if (qrl.levelName.equalsIgnoreCase(level)) {
                return qrl;
            }
        }
        throw new IllegalArgumentException("Unknown QueryRetrieveLevel: " + level);
    }
}
