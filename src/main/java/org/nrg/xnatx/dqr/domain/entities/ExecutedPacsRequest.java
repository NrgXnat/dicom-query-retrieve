/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

import java.util.Date;
import java.util.List;

import jakarta.persistence.Cacheable;
import jakarta.persistence.Entity;
import jakarta.persistence.NamedNativeQueries;
import jakarta.persistence.NamedNativeQuery;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Temporal;
import jakarta.persistence.TemporalType;

@Entity
@Cacheable
@NoArgsConstructor
@NamedQueries({
        @NamedQuery(name = ExecutedPacsRequest.GET_BY_STUDY_INSTANCE_UID_AND_REQUEST_ID_ORDERED_BY_MOST_RECENT,
                query = "SELECT r FROM ExecutedPacsRequest r WHERE r.studyInstanceUid = :studyInstanceUid AND r.requestId = :requestId ORDER BY r.executedTime DESC"),
})
@NamedNativeQueries({
        @NamedNativeQuery(name = ExecutedPacsRequest.DELETE_SERIES_IDS_WITH_REQUEST_ID_AND_STATUS,
                query = "DELETE FROM xhbm_executed_pacs_request_series_ids "
                      + "WHERE executed_pacs_request IN ("
                      + "  SELECT id FROM xhbm_executed_pacs_request "
                      + "  WHERE request_id = :requestId AND status IN (:statuses))"),
        @NamedNativeQuery(name = ExecutedPacsRequest.DELETE_ALL_WITH_REQUEST_ID_AND_STATUS,
                query = "DELETE FROM xhbm_executed_pacs_request "
                      + "WHERE request_id = :requestId AND status IN (:statuses)"),
})
public class ExecutedPacsRequest extends PacsRequest {
    private static final long serialVersionUID = -2942642818163500573L;

    public static final String GET_BY_STUDY_INSTANCE_UID_AND_REQUEST_ID_ORDERED_BY_MOST_RECENT = "ExecutedPacsRequest.getByStudyInstanceUidAndRequestIdOrderedByMostRecent";
    public static final String DELETE_SERIES_IDS_WITH_REQUEST_ID_AND_STATUS                   = "ExecutedPacsRequest.deleteSeriesIdsWithRequestIdAndStatus";
    public static final String DELETE_ALL_WITH_REQUEST_ID_AND_STATUS                          = "ExecutedPacsRequest.deleteAllWithRequestIdAndStatus";

    @Builder
    public ExecutedPacsRequest(final String username, final Long pacsId, final String xnatProject, final String studyInstanceUid, final List<String> seriesIds, final String remappingScript, final String destinationAeTitle, final String status, final Long priority, final Date queuedTime, final Date executedTime, final String studyDate, final String studyId, final String accessionNumber, final String patientId, final String patientName, final String errorMessage, final String requestId, final String subjectLabel, final String experimentLabel) {
        super(username, pacsId, xnatProject, studyInstanceUid, seriesIds, remappingScript, destinationAeTitle, status, priority, queuedTime, studyDate, studyId, accessionNumber, patientId, patientName, errorMessage, requestId, subjectLabel, experimentLabel);
        _executedTime = executedTime;
    }

    public Date getExecutedTime() {
        return _executedTime;
    }

    public void setExecutedTime(final Date executedTime) {
        _executedTime = executedTime;
    }

    @Override
    public String toString() {
        return "{ username: " + getUsername() + ", "
               + "pacsId: " + getPacsId() + ", "
               + "xnatProject: " + getXnatProject() + ", "
               + "studyInstanceUid: " + getStudyInstanceUid() + ", "
               + "seriesIds: " + getSeriesIds() + ", "
               + "remappingScript: " + getRemappingScript() + ", "
               + "destinationAeTitle: " + getDestinationAeTitle() + ", "
               + "status: " + getStatus() + ", "
               + "queuedTime: " + getQueuedTime() + ", "
               + "executedTime: " + getExecutedTime() + ", "
               + "priority: " + getPriority() + ", "
               + "studyDate: " + getStudyDate() + ", "
               + "studyId: " + getStudyId() + ", "
               + "accessionNumber: " + getAccessionNumber() + ", "
               + "patientId: " + getPatientId() + ", "
               + "patientName: " + getPatientName() + ", "
               + "errorMessage: " + getErrorMessage() + ", "
               + "requestId: " + getRequestId() + "}";
    }

    @Temporal(TemporalType.TIMESTAMP)
    private Date _executedTime;
}
