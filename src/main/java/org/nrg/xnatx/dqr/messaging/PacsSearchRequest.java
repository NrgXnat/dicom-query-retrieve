/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.messaging.PacsSearchRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.messaging;

import lombok.Builder;
import lombok.Singular;
import lombok.Value;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Value
@Builder
public class PacsSearchRequest implements Serializable {
    private static final long serialVersionUID = -1150137478832020599L;

    public enum Type {
        PatientsByExample,
        PatientById,
        StudiesByExample,
        StudyById,
        SeriesByStudy,
        SeriesByStudyUid,
        SeriesById,
        Unknown
    }

    @Builder.Default
    UUID searchId = UUID.randomUUID();

    String username;
    Long   pacsId;
    Type   searchType;
    String project;
    String remappingScript;
    String destinationAeTitle;
    String status;
    Long   priority;
    Date   queuedTime;
    String studyDate;
    String accessionNumber;
    String patientId;
    String patientName;
    String modality;
    String dob;
    Date   startDate;
    Date   endDate;
    @Singular
    List<String> studyIds;
    @Singular
    List<String> studyInstanceUids;
    @Singular
    List<String> seriesIds;
    @Singular
    List<String> seriesInstanceUids;
}
