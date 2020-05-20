/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.ExecutedPacsRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.util.Date;
import java.util.List;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class ExecutedPacsRequest extends PacsRequest {
    public ExecutedPacsRequest() {
        super();
    }

    public ExecutedPacsRequest(final String username, final Long pacsId, final String xnatProject, final String studyInstanceUid, final List<String> seriesIds, final String remappingScript, final String destinationAeTitle, final String status, final Long priority, final Date queuedTime, final Date executedTime, final String studyDate, final String studyId, final String accessionNumber, final String patientId, final String patientName) {
        super(username, pacsId, xnatProject, studyInstanceUid, seriesIds, remappingScript, destinationAeTitle, status, priority, queuedTime, studyDate, studyId, accessionNumber, patientId, patientName);
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
               + "patientName: " + getPatientName() + "}";
    }

    @Temporal(TemporalType.TIMESTAMP)
    private Date _executedTime;
}
