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
    protected String _remappingScript;
    protected String _destinationAeTitle;
    protected String _status;
    protected Long _priority;
    protected Date _queuedTime;
    protected String _studyDate;
    protected String _studyId;
    protected String _accessionNumber;
    protected String _patientId;
    protected String _patientName;

    public static final String QUEUED_STATUS_TEXT = "QUEUED";
    public static final String PROCESSING_STATUS_TEXT = "PROCESSING";
    public static final String ISSUED_STATUS_TEXT = "ISSUED";
    public static final String FAILED_STATUS_TEXT = "FAILED";
    public static final String RECEIVED_STATUS_TEXT = "RECEIVED";
    public static final Long HIGH_PRIORITY = 1L;
    public static final Long STANDARD_PRIORITY = 10L;

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

    @Column(columnDefinition = "TEXT")
    public String getRemappingScript() {
        return _remappingScript;
    }

    public void setRemappingScript(String remappingScript) {
        this._remappingScript = remappingScript;
    }

    public String getStatus() {
        return _status;
    }

    public void setStatus(String _status) {
        this._status = _status;
    }

    public Long getPriority() {
        return _priority;
    }

    public void setPriority(Long _priority) {
        this._priority = _priority;
    }

    public String getStudyDate() {
        return _studyDate;
    }

    public void setStudyDate(String _studyDate) {
        this._studyDate = _studyDate;
    }

    public String getStudyId() {
        return _studyId;
    }

    public void setStudyId(String _studyId) {
        this._studyId = _studyId;
    }

    public String getAccessionNumber() {
        return _accessionNumber;
    }

    public void setAccessionNumber(String _accessionNumber) {
        this._accessionNumber = _accessionNumber;
    }

    public String getPatientId() {
        return _patientId;
    }

    public void setPatientId(String _patientId) {
        this._patientId = _patientId;
    }

    public String getPatientName() {
        return _patientName;
    }

    public void setPatientName(String _patientName) {
        this._patientName = _patientName;
    }
}
