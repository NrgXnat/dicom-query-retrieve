package org.nrg.dqr.domain.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import javax.persistence.Temporal;
import javax.persistence.TemporalType;
import java.io.Serializable;
import java.util.Date;

/**
 * Created by mike on 1/23/18.
 */
@MappedSuperclass
@AllArgsConstructor
@NoArgsConstructor
public class PacsRequest extends AbstractHibernateEntity implements Serializable {
    public static final String QUEUED_STATUS_TEXT     = "QUEUED";
    public static final String PROCESSING_STATUS_TEXT = "PROCESSING";
    public static final String ISSUED_STATUS_TEXT     = "ISSUED";
    public static final String FAILED_STATUS_TEXT     = "FAILED";
    public static final String RECEIVED_STATUS_TEXT   = "RECEIVED";
    public static final Long   HIGH_PRIORITY          = 1L;
    public static final Long   STANDARD_PRIORITY      = 10L;

    public String getDestinationAeTitle() {
        return _destinationAeTitle;
    }

    public void setDestinationAeTitle(final String destinationAeTitle) {
        _destinationAeTitle = destinationAeTitle;
    }

    @Temporal(TemporalType.TIMESTAMP)
    public Date getQueuedTime() {
        return _queuedTime;
    }

    public void setQueuedTime(final Date queuedTime) {
        _queuedTime = queuedTime;
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(final String username) {
        _username = username;
    }

    public Long getPacsId() {
        return _pacsId;
    }

    public void setPacsId(final Long pacsId) {
        _pacsId = pacsId;
    }

    public String getXnatProject() {
        return _xnatProject;
    }

    public void setXnatProject(final String xnatProject) {
        _xnatProject = xnatProject;
    }

    public String getStudyInstanceUid() {
        return _studyInstanceUid;
    }

    public void setStudyInstanceUid(final String studyId) {
        _studyInstanceUid = studyId;
    }

    @Column(columnDefinition = "TEXT")
    public String getSeriesIds() {
        return _seriesIds;
    }

    public void setSeriesIds(final String seriesIds) {
        _seriesIds = seriesIds;
    }

    @Column(columnDefinition = "TEXT")
    public String getRemappingScript() {
        return _remappingScript;
    }

    public void setRemappingScript(final String remappingScript) {
        _remappingScript = remappingScript;
    }

    public String getStatus() {
        return _status;
    }

    public void setStatus(final String status) {
        _status = status;
    }

    public Long getPriority() {
        return _priority;
    }

    public void setPriority(final Long priority) {
        _priority = priority;
    }

    public String getStudyDate() {
        return _studyDate;
    }

    public void setStudyDate(final String studyDate) {
        _studyDate = studyDate;
    }

    public String getStudyId() {
        return _studyId;
    }

    public void setStudyId(final String studyId) {
        _studyId = studyId;
    }

    public String getAccessionNumber() {
        return _accessionNumber;
    }

    public void setAccessionNumber(final String accessionNumber) {
        _accessionNumber = accessionNumber;
    }

    public String getPatientId() {
        return _patientId;
    }

    public void setPatientId(final String patientId) {
        _patientId = patientId;
    }

    public String getPatientName() {
        return _patientName;
    }

    public void setPatientName(final String patientName) {
        _patientName = patientName;
    }

    @Override
    public boolean equals(final Object object) {
        if (this == object) {
            return true;
        }

        if (object == null || getClass() != object.getClass()) {
            return false;
        }

        final PacsRequest that = (PacsRequest) object;

        return new EqualsBuilder()
                .appendSuper(super.equals(object))
                .append(getUsername(), that.getUsername())
                .append(getPacsId(), that.getPacsId())
                .append(getXnatProject(), that.getXnatProject())
                .append(getStudyInstanceUid(), that.getStudyInstanceUid())
                .append(getSeriesIds(), that.getSeriesIds())
                .append(getRemappingScript(), that.getRemappingScript())
                .append(getDestinationAeTitle(), that.getDestinationAeTitle())
                .append(getStatus(), that.getStatus())
                .append(getPriority(), that.getPriority())
                .append(getQueuedTime(), that.getQueuedTime())
                .append(getStudyDate(), that.getStudyDate())
                .append(getStudyId(), that.getStudyId())
                .append(getAccessionNumber(), that.getAccessionNumber())
                .append(getPatientId(), that.getPatientId())
                .append(getPatientName(), that.getPatientName())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .appendSuper(super.hashCode())
                .append(getUsername())
                .append(getPacsId())
                .append(getXnatProject())
                .append(getStudyInstanceUid())
                .append(getSeriesIds())
                .append(getRemappingScript())
                .append(getDestinationAeTitle())
                .append(getStatus())
                .append(getPriority())
                .append(getQueuedTime())
                .append(getStudyDate())
                .append(getStudyId())
                .append(getAccessionNumber())
                .append(getPatientId())
                .append(getPatientName())
                .toHashCode();
    }

    @Override
    public String toString() {
        return String.format(FORMAT, getClass().getName(), getUsername(), getPacsId(), getXnatProject(), getStudyInstanceUid(), getSeriesIds(), getRemappingScript(), getDestinationAeTitle(), getStatus(), getQueuedTime(), getPriority(), getStudyDate(), getStudyId(), getAccessionNumber(), getPatientId(), getPatientName() + " }");
    }

    private static final String FORMAT = "{ \"type\": \"%s\", \"username\": \"%s\", \"pacsId\": %d, \"xnatProject\": \"%s\", \"studyInstanceUid\": \"%s\", \"seriesIds\": \"%s\", \"remappingScript\": \"%s\", \"destinationAeTitle\": \"%s\", \"status\": \"%s\", \"queuedTime\": \"%s\", \"priority\": %d, \"studyDate\": \"%s\", \"studyId\": \"%s\", \"accessionNumber\": \"%s\", \"patientId\": \"%s\", \"patientName\": \"%s\": }";

    private String _username;
    private Long   _pacsId;
    private String _xnatProject;
    private String _studyInstanceUid;
    private String _seriesIds;
    private String _remappingScript;
    private String _destinationAeTitle;
    private String _status;
    private Long   _priority;
    private Date   _queuedTime;
    private String _studyDate;
    private String _studyId;
    private String _accessionNumber;
    private String _patientId;
    private String _patientName;
}
