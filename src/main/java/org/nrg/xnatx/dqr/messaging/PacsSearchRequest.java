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
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Value
@Accessors(prefix = "_")
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
    UUID _searchId = UUID.randomUUID();

    String _username;
    Long   _pacsId;
    Type   _searchType;
    String _project;
    String _remappingScript;
    String _destinationAeTitle;
    String _status;
    Long   _priority;
    Date   _queuedTime;
    String _studyDate;
    String _accessionNumber;
    String _patientId;
    String _patientName;
    String _modality;
    String _dob;
    Date   _startDate;
    Date   _endDate;
    @Singular
    List<String> _studyIds;
    @Singular
    List<String> _studyInstanceUids;
    @Singular
    List<String> _seriesIds;
    @Singular
    List<String> _seriesInstanceUids;
}
