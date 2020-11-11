package org.nrg.dqr.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Objects;

/**
 * Created by mike on 1/23/18.
 */
@Entity
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@AllArgsConstructor
@NoArgsConstructor
@Builder
@Accessors(prefix = "_")
public class StudyIdStudyInstanceUidMapping extends AbstractHibernateEntity implements Serializable {
    public String getStudyId() {
        return _studyId;
    }

    public void setStudyId(final String studyId) {
        _studyId = studyId;
    }

    public String getStudyInstanceUid() {
        return _studyInstanceUid;
    }

    public void setStudyInstanceUid(final String studyInstanceUid) {
        _studyInstanceUid = studyInstanceUid;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }
        if (object == null || getClass() != object.getClass()) {
            return false;
        }
        if (!super.equals(object)) {
            return false;
        }

        final StudyIdStudyInstanceUidMapping that = (StudyIdStudyInstanceUidMapping) object;

        if (!Objects.equals(_studyId, that._studyId)) {
            return false;
        }
        return Objects.equals(_studyInstanceUid, that._studyInstanceUid);
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + (_studyId != null ? _studyId.hashCode() : 0);
        result = 31 * result + (_studyInstanceUid != null ? _studyInstanceUid.hashCode() : 0);
        return result;
    }

    @Override
    public String toString() {
        return "StudyIdStudyInstanceUidMapping{" +
               "_studyId='" + _studyId + '\'' +
               ", _studyInstanceUid='" + _studyInstanceUid + '\'' +
               '}';
    }

    private String _studyId;
    private String _studyInstanceUid;
}
