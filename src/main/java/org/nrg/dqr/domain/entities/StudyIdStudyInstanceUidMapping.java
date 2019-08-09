package org.nrg.dqr.domain.entities;

import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Column;
import javax.persistence.Entity;
import javax.persistence.MappedSuperclass;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by mike on 1/23/18.
 */
@Entity
@Table
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class StudyIdStudyInstanceUidMapping extends AbstractHibernateEntity implements Serializable {
    protected static final long serialVersionUID = 1L;

    private String _studyId;
    private String _studyInstanceUid;

    // for Hibernate
    public StudyIdStudyInstanceUidMapping() {
    }

    public String getStudyId() {
        return _studyId;
    }

    public void setStudyId(String _studyId) {
        this._studyId = _studyId;
    }

    public String getStudyInstanceUid() {
        return _studyInstanceUid;
    }

    public void setStudyInstanceUid(String _studyInstanceUid) {
        this._studyInstanceUid = _studyInstanceUid;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        StudyIdStudyInstanceUidMapping that = (StudyIdStudyInstanceUidMapping) o;

        if (_studyId != null ? !_studyId.equals(that._studyId) : that._studyId != null) return false;
        return _studyInstanceUid != null ? _studyInstanceUid.equals(that._studyInstanceUid) : that._studyInstanceUid == null;
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
}
