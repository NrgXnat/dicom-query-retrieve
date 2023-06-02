/**
 * Copyright (c) 2023 Flywheel
 */
package org.nrg.xnatx.dqr.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.apache.commons.lang3.StringUtils;
import org.dcm4che2.data.DicomObject;
import org.dcm4che2.data.Tag;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Basic;
import javax.persistence.Entity;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import java.util.Objects;

@Entity
@Table(uniqueConstraints = {@UniqueConstraint(columnNames = {"studyInstanceUid", "seriesInstanceUid", "project"})})
@Getter @Setter
@NoArgsConstructor @AllArgsConstructor @Builder
public final class SeriesRetrievalStatus extends AbstractHibernateEntity {
    private String username;
    private String studyInstanceUid;
    private String seriesInstanceUid;
    private String seriesNumber;
    private String seriesDescription;
    private String project;
    @Basic(optional = true) private Integer numberOfRelatedInstances;
    @Builder.Default private int numberOfDownloadedInstances = 0;
    @Builder.Default private long bytesDownloaded = 0;

    @Override
    public boolean equals(final Object o) {
        if (null == o) {
            return false;
        } else if (this == o) {
            return true;
        } else if (o instanceof SeriesRetrievalStatus) {
            final SeriesRetrievalStatus e = (SeriesRetrievalStatus) o;
            return Objects.equals(studyInstanceUid, e.studyInstanceUid) &&
                    Objects.equals(seriesInstanceUid, e.seriesInstanceUid) &&
                    Objects.equals(project, e.project);
        } else {
            return false;
        }
    }

    public static SeriesRetrievalStatus fromCFindResult(final String username, final String project, final DicomObject o) {
        final String studyInstanceUid = o.getString(Tag.StudyInstanceUID);
        if (StringUtils.isEmpty(studyInstanceUid)) {
            throw new IllegalArgumentException("Cannot create series retrieval record with no Study Instance UID");
        }
        final String seriesInstanceUid = o.getString(Tag.SeriesInstanceUID);
        if (StringUtils.isEmpty(seriesInstanceUid)) {
            throw new IllegalArgumentException("Cannot create series retrieval record with no Series Instance UID");
        }
        final int numberOfRelatedInstances = o.getInt(Tag.NumberOfSeriesRelatedInstances, -1);
        final SeriesRetrievalStatusBuilder builder = builder()
                .username(username)
                .studyInstanceUid(studyInstanceUid)
                .seriesInstanceUid(seriesInstanceUid)
                .seriesNumber(o.getString(Tag.SeriesNumber))
                .seriesDescription(o.getString(Tag.SeriesDescription))
                .project(project);
        if (numberOfRelatedInstances > 0) {
            builder.numberOfRelatedInstances(numberOfRelatedInstances);
        }
        return builder.build();
    }
}
