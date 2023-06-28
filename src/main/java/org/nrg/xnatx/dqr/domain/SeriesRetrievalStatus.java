/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.domain;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Getter;
import org.nrg.xnatx.dqr.domain.entities.ArchivedRequestedSeries;
import org.nrg.xnatx.dqr.domain.entities.SeriesRetrievalRequest;

import javax.annotation.Nullable;
import java.util.Date;

/**
 * Merged information from a series retrieval request and (optional) corresponding archived requested series,
 * for presentation through the /dqr/import/status HTTP endpoint.
 */
@Getter
public class SeriesRetrievalStatus {
    @JsonFormat(shape = JsonFormat.Shape.STRING)
    private final Date created;
    private final String studyInstanceUid;
    private final String patientId;
    private final String studyId;
    private final String seriesInstanceUid;
    private final String modality;
    private final String seriesNumber;
    private final String requestingUser;
    private final @Nullable String userDefinedId;
    private final @Nullable Integer expectedInstances;
    private final int instancesArchived;
    private final long bytesArchived;

    public SeriesRetrievalStatus(final SeriesRetrievalRequest request, final @Nullable ArchivedRequestedSeries archived) {
        created = request.getCreated();
        requestingUser = request.getRequestingUser();
        userDefinedId = request.getUserDefinedId();
        studyInstanceUid = request.getStudyInstanceUid();
        patientId = request.getPatientId();
        studyId = request.getStudyId();
        seriesInstanceUid = request.getSeriesInstanceUid();
        modality = request.getModality();
        seriesNumber = request.getSeriesNumber();
        expectedInstances = request.getExpectedInstances();
        instancesArchived = null == archived ? 0 : archived.getInstancesArchived();
        bytesArchived = null == archived ? 0 : archived.getBytesArchived();
    }
}
