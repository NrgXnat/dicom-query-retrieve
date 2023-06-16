/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;

/**
 * Log record of a user's request for one series. This is intended to
 * be an immutable trace of each request made; status of requested series
 * can be determined by comparing these requests against ArchivedRequestedSeries
 * records.
 */
@Entity
@Builder
@NoArgsConstructor @AllArgsConstructor
@Getter @Setter
public class SeriesRetrievalRequest extends AbstractHibernateEntity {
    private String requestingUser;
    private String studyInstanceUid;
    private String seriesInstanceUid;
    private String destinationProject;
    private String patientId;
    private String studyId;
    private String modality;
    private String seriesNumber;
    private Integer expectedInstances;
}
