/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.dicom.command.dcm4che3.QueryRetrieveLevel
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2024, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.dicom.command.dcm4che3;

import org.dcm4che3.data.Tag;
import org.dcm4che3.data.UID;

import java.util.Arrays;
import java.util.List;

/**
 * Query/Retrieve levels for DICOM C-FIND and C-MOVE operations.
 * Replaces org.dcm4che2.tool.dcmqr.DcmQR.QueryRetrieveLevel
 */
public enum QueryRetrieveLevel {

    PATIENT("PATIENT",
            UID.PatientRootQueryRetrieveInformationModelFind,
            UID.PatientRootQueryRetrieveInformationModelMove,
            Arrays.asList(Tag.PatientID, Tag.PatientName, Tag.PatientBirthDate, Tag.PatientSex)),

    STUDY("STUDY",
            UID.StudyRootQueryRetrieveInformationModelFind,
            UID.StudyRootQueryRetrieveInformationModelMove,
            Arrays.asList(Tag.StudyInstanceUID, Tag.StudyID, Tag.StudyDate, Tag.StudyTime,
                    Tag.AccessionNumber, Tag.PatientID, Tag.PatientName)),

    SERIES("SERIES",
            UID.StudyRootQueryRetrieveInformationModelFind,
            UID.StudyRootQueryRetrieveInformationModelMove,
            Arrays.asList(Tag.SeriesInstanceUID, Tag.SeriesNumber, Tag.Modality,
                    Tag.SeriesDescription, Tag.StudyInstanceUID)),

    IMAGE("IMAGE",
            UID.StudyRootQueryRetrieveInformationModelFind,
            UID.StudyRootQueryRetrieveInformationModelMove,
            Arrays.asList(Tag.SOPInstanceUID, Tag.SOPClassUID, Tag.InstanceNumber,
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

    /**
     * Returns the level name as used in DICOM QueryRetrieveLevel attribute.
     */
    public String getLevelName() {
        return levelName;
    }

    /**
     * Returns the SOP Class UID for C-FIND operations at this level.
     */
    public String getFindSopClass() {
        return findSopClass;
    }

    /**
     * Returns the SOP Class UID for C-MOVE operations at this level.
     */
    public String getMoveSopClass() {
        return moveSopClass;
    }

    /**
     * Returns the default return keys for this query level.
     */
    public List<Integer> getDefaultReturnKeys() {
        return defaultReturnKeys;
    }

    /**
     * Parses a level name string to the corresponding enum value.
     */
    public static QueryRetrieveLevel fromString(String level) {
        for (QueryRetrieveLevel qrl : values()) {
            if (qrl.levelName.equalsIgnoreCase(level)) {
                return qrl;
            }
        }
        throw new IllegalArgumentException("Unknown QueryRetrieveLevel: " + level);
    }
}
