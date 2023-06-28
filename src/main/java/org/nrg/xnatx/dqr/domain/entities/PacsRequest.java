/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.PacsRequest
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import java.io.Serializable;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import javax.annotation.Nullable;
import javax.persistence.*;

/**
 * Created by mike on 1/23/18.
 */
@MappedSuperclass
public class PacsRequest extends AbstractHibernateEntity implements Serializable {
    private static final long serialVersionUID = -4904269689415210630L;

    public static final String QUEUED_STATUS_TEXT     = "QUEUED";
    public static final String PROCESSING_STATUS_TEXT = "PROCESSING";
    public static final String ISSUED_STATUS_TEXT     = "ISSUED";
    public static final String FAILED_STATUS_TEXT     = "FAILED";
    public static final String RECEIVED_STATUS_TEXT   = "RECEIVED";
    public static final Long   HIGH_PRIORITY          = 1L;
    public static final Long   STANDARD_PRIORITY      = 10L;

    public PacsRequest() {
        super();
    }

    public PacsRequest(final String username, final @Nullable String userDefinedId, final Long pacsId, final String xnatProject, final String studyInstanceUid, final List<String> seriesIds, final String remappingScript, final String destinationAeTitle, final String status, final Long priority, final Date queuedTime, final String studyDate, final String studyId, final String accessionNumber, final String patientId, final String patientName) {
        _username           = username;
        _userDefinedId      = userDefinedId;
        _pacsId             = pacsId;
        _xnatProject        = xnatProject;
        _studyInstanceUid   = studyInstanceUid;
        _seriesIds          = new ArrayList<>(seriesIds);
        _remappingScript    = remappingScript;
        _destinationAeTitle = destinationAeTitle;
        _status             = status;
        _priority           = priority;
        _queuedTime         = queuedTime;
        _studyDate          = studyDate;
        _studyId            = studyId;
        _accessionNumber    = accessionNumber;
        _patientId          = patientId;
        _patientName        = patientName;
    }

    @Transient
    public String getDecodedAeAndPort() {
        final String aeAndPort = getDestinationAeTitle();
        try {
            return URLDecoder.decode(aeAndPort, "UTF-8");
        } catch (Exception ignored) {
            return aeAndPort;
        }
    }

    public String getUsername() {
        return _username;
    }

    public void setUsername(final String username) {
        _username = username;
    }

    public @Nullable String getUserDefinedId() { return _userDefinedId; }

    public void setUserDefinedId(final @Nullable String userDefinedId) { _userDefinedId = userDefinedId; }

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

    public void setStudyInstanceUid(final String studyInstanceUid) {
        _studyInstanceUid = studyInstanceUid;
    }

    @ElementCollection(targetClass = String.class, fetch = FetchType.EAGER)
    @Fetch(FetchMode.SUBSELECT)
    public List<String> getSeriesIds() {
        return _seriesIds;
    }

    public void setSeriesIds(final List<String> seriesIds) {
        _seriesIds = seriesIds;
    }

    @Column(length = 16384)
    public String getRemappingScript() {
        return _remappingScript;
    }

    public void setRemappingScript(final String remappingScript) {
        _remappingScript = remappingScript;
    }

    public String getDestinationAeTitle() {
        return _destinationAeTitle;
    }

    public void setDestinationAeTitle(final String destinationAeTitle) {
        _destinationAeTitle = destinationAeTitle;
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

    @Temporal(TemporalType.TIMESTAMP)
    public Date getQueuedTime() {
        return _queuedTime;
    }

    public void setQueuedTime(final Date queuedTime) {
        _queuedTime = queuedTime;
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
               + "priority: " + getPriority() + ", "
               + "studyDate: " + getStudyDate() + ", "
               + "studyId: " + getStudyId() + ", "
               + "accessionNumber: " + getAccessionNumber() + ", "
               + "patientId: " + getPatientId() + ", "
               + "patientName: " + getPatientName() + "}";
    }

    private String       _username;
    private @Nullable String _userDefinedId;
    private Long         _pacsId;
    private String       _xnatProject;
    private String       _studyInstanceUid;
    private List<String> _seriesIds;
    private String       _remappingScript;
    private String       _destinationAeTitle;
    private String       _status;
    private Long         _priority;
    private Date         _queuedTime;
    private String       _studyDate;
    private String       _studyId;
    private String       _accessionNumber;
    private String       _patientId;
    private String       _patientName;
}
