package org.nrg.dqr.domain.entities;

import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by mike on 1/23/18.
 */
@MappedSuperclass
public class PacsRequest extends AbstractHibernateEntity implements Serializable {
    protected static final long serialVersionUID = 1L;

    protected String _username;
    protected Long _pacsId;
    protected String _xnatProject;
    protected String _studyInstanceUid;
    protected String _seriesIds;

    protected String _destinationAeTitle;

    protected Date _queuedTime;

    public String getDestinationAeTitle() {
        return _destinationAeTitle;
    }

    public void setDestinationAeTitle(String _destinationAeTitle) {
        this._destinationAeTitle = _destinationAeTitle;
    }

    public Date getQueuedTime() {
        return _queuedTime;
    }

    public void setQueuedTime(Date _queuedTime) {
        this._queuedTime = _queuedTime;
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(String _username) {
        this._username = _username;
    }

    public Long getPacsId() {
        return _pacsId;
    }

    public void setPacsId(Long _pacsId) {
        this._pacsId = _pacsId;
    }

    public String getXnatProject() {
        return _xnatProject;
    }

    public void setXnatProject(String _xnatProject) {
        this._xnatProject = _xnatProject;
    }

    public String getStudyInstanceUid() {
        return _studyInstanceUid;
    }

    public void setStudyInstanceUid(String _studyId) {
        this._studyInstanceUid = _studyId;
    }

    @Column(columnDefinition = "TEXT")
    public String getSeriesIds() {
        return _seriesIds;
    }

    public void setSeriesIds(String _seriesIds) {
        this._seriesIds = _seriesIds;
    }
}
