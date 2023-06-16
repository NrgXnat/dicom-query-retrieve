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
 * Record of one fulfilled request for a download from PACS to XNAT.
 */
@Entity
@NoArgsConstructor @AllArgsConstructor @Builder
@Getter @Setter
public class ArchivedRequestedSeries extends AbstractHibernateEntity {
    private String studyInstanceUid;
    private String seriesInstanceUid;
    private String xnatProject;
    private int instancesArchived;
    private long bytesArchived;
}
