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
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;

import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.Entity;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;

@Entity
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@NoArgsConstructor
public class ExecutedPacsRequest extends PacsRequest {
    private static final long serialVersionUID = -2942642818163500573L;

    @Builder
    public ExecutedPacsRequest(final String username, final @Nullable String userDefinedId, final Long pacsId, final String xnatProject, final String studyInstanceUid, final List<String> seriesIds, final String remappingScript, final String destinationAeTitle, final String status, final Long priority, final Date queuedTime, final Date executedTime, final String studyDate, final String studyId, final String accessionNumber, final String patientId, final String patientName) {
        super(username, userDefinedId, pacsId, xnatProject, studyInstanceUid, seriesIds, remappingScript, destinationAeTitle, status, priority, queuedTime, studyDate, studyId, accessionNumber, patientId, patientName);
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
                + "userDefinedId: " + getUserDefinedId() + ", "
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
