/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.StudyIdStudyInstanceUidMapping
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import java.io.Serializable;
import java.util.Objects;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

/**
 * Created by mike on 1/23/18.
 */
@Entity
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@Getter
@Setter
@Accessors(prefix = "_")
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class StudyIdStudyInstanceUidMapping extends AbstractHibernateEntity implements Serializable {
    @Override
    public boolean equals(final Object object) {
        if (!(object instanceof StudyIdStudyInstanceUidMapping) || !super.equals(object)) {
            return false;
        }
        if (this == object) {
            return true;
        }
        final StudyIdStudyInstanceUidMapping that = (StudyIdStudyInstanceUidMapping) object;
        return StringUtils.equals(_studyId, that._studyId) && StringUtils.equals(_studyInstanceUid, that._studyInstanceUid);
    }

    @Override
    public int hashCode() {
        return Objects.hash(super.hashCode(), getStudyId(), getStudyInstanceUid());
    }

    @Override
    public String toString() {
        return String.format(FORMAT, _studyId, _studyInstanceUid);
    }

    private static final String FORMAT = "Study ID: %s, study instance UID: %s";

    private String _studyId;
    private String _studyInstanceUid;
}
