/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.QueuedPacsRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import javax.persistence.Column;
import lombok.Builder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.NamedQueries;
import javax.persistence.NamedQuery;
import javax.persistence.Table;
import java.util.Date;
import java.util.List;

@Entity
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@NamedQueries({
        @NamedQuery(name = QueuedPacsRequest.IS_QUEUED_FOR_STUDY_INSTANCE_UID_AND_REQUEST_ID,
                query = "SELECT (count(q.id) > 0) FROM QueuedPacsRequest q WHERE q.studyInstanceUid = :studyInstanceUid AND q.requestId = :requestId"),
})
public class QueuedPacsRequest extends PacsRequest {
    private static final long serialVersionUID = 7081993254603730636L;

    public static final String IS_QUEUED_FOR_STUDY_INSTANCE_UID_AND_REQUEST_ID = "QueuedPacsRequest.isQueuedForStudyInstanceUidAndRequestId";

    public QueuedPacsRequest() {
        super();
    }

    @Builder
    public QueuedPacsRequest(final String username, final Long pacsId, final String xnatProject, final String studyInstanceUid, final List<String> seriesIds, final String remappingScript, final String destinationAeTitle, final String status, final Long priority, final Date queuedTime, final String studyDate, final String studyId, final String accessionNumber, final String patientId, final String patientName, final String errorMessage, final String requestId) {
        super(username, pacsId, xnatProject, studyInstanceUid, seriesIds, remappingScript, destinationAeTitle, status, priority, queuedTime, studyDate, studyId, accessionNumber, patientId, patientName, errorMessage, requestId);
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
