/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import javax.persistence.Cacheable;
import javax.persistence.Column;
import lombok.Builder;

import javax.persistence.Entity;
import javax.persistence.NamedNativeQueries;
import javax.persistence.NamedNativeQuery;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;

@Entity
@Table
@Cacheable
@NamedQueries({
        @NamedQuery(name = QueuedPacsRequest.IS_QUEUED_FOR_STUDY_INSTANCE_UID_AND_REQUEST_ID,
                query = "SELECT (count(q.id) > 0) FROM QueuedPacsRequest q WHERE q.studyInstanceUid = :studyInstanceUid AND q.requestId = :requestId"),
        @NamedQuery(name = QueuedPacsRequest.IS_QUEUED_FOR_STUDY_INSTANCE_UID,
                query = "SELECT (count(q.id) > 0) FROM QueuedPacsRequest q WHERE q.studyInstanceUid = :studyInstanceUid"),
})
@NamedNativeQueries({
        @NamedNativeQuery(name = QueuedPacsRequest.DELETE_SERIES_IDS_WITH_REQUEST_ID_AND_STATUS,
                query = "DELETE FROM xhbm_queued_pacs_request_series_ids "
                      + "WHERE queued_pacs_request IN ("
                      + "  SELECT id FROM xhbm_queued_pacs_request "
                      + "  WHERE request_id = :requestId AND status IN (:statuses))"),
        @NamedNativeQuery(name = QueuedPacsRequest.DELETE_ALL_WITH_REQUEST_ID_AND_STATUS,
                query = "DELETE FROM xhbm_queued_pacs_request "
                      + "WHERE request_id = :requestId AND status IN (:statuses)"),
})
public class QueuedPacsRequest extends PacsRequest {
    private static final long serialVersionUID = 7081993254603730636L;

    public static final String IS_QUEUED_FOR_STUDY_INSTANCE_UID_AND_REQUEST_ID = "QueuedPacsRequest.isQueuedForStudyInstanceUidAndRequestId";
    public static final String IS_QUEUED_FOR_STUDY_INSTANCE_UID                = "QueuedPacsRequest.isQueuedForStudyInstanceUid";
    public static final String DELETE_SERIES_IDS_WITH_REQUEST_ID_AND_STATUS     = "QueuedPacsRequest.deleteSeriesIdsWithRequestIdAndStatus";
    public static final String DELETE_ALL_WITH_REQUEST_ID_AND_STATUS            = "QueuedPacsRequest.deleteAllWithRequestIdAndStatus";

    public QueuedPacsRequest() {
        super();
    }

    @Builder
    public QueuedPacsRequest(final String username, final Long pacsId, final String xnatProject, final String studyInstanceUid, final List<String> seriesIds, final String remappingScript, final String destinationAeTitle, final String status, final Long priority, final Date queuedTime, final String studyDate, final String studyId, final String accessionNumber, final String patientId, final String patientName, final String errorMessage, final String requestId, final String subjectLabel, final String experimentLabel) {
        super(username, pacsId, xnatProject, studyInstanceUid, seriesIds, remappingScript, destinationAeTitle, status, priority, queuedTime, studyDate, studyId, accessionNumber, patientId, patientName, errorMessage, requestId, subjectLabel, experimentLabel);
    }


    @Column(columnDefinition = "int default 0")
    public Integer getRetries() {
        return _retries;
    }

    public void setRetries(final Integer retries) {
        _retries = retries;
    }

    private Integer _retries;
}
