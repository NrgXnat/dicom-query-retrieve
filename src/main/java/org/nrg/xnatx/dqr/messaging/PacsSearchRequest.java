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
import lombok.Data;
import lombok.experimental.Accessors;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Data
@Accessors(prefix = "_")
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

    @Builder
    public PacsSearchRequest(final String username, final Long pacsId, final String project, final String studyInstanceUid, final List<String> seriesIds, final String remappingScript, final String destinationAeTitle, final String status, final Long priority, final Date queuedTime, final String studyDate, final String studyId, final String accessionNumber, final String patientId, final String patientName, final Type searchType, final String modality, final String dob, final Date startDate, final Date endDate, final List<String> studyInstanceUids) {
        _searchId = UUID.randomUUID();
        _username = username;
        _pacsId = pacsId;
        _searchType = searchType;
        _studyInstanceUids = studyInstanceUids;
    }

    private final UUID         _searchId;
    private final String       _username;
    private final Long         _pacsId;
    private final Type         _searchType;
    private final List<String> _studyInstanceUids;
}
