package org.nrg.xnatx.dqr.domain.entities;

import java.io.Serializable;
import java.util.Date;
import java.util.List;
import javax.persistence.Column;
import javax.persistence.MappedSuperclass;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;
import org.apache.commons.lang3.StringUtils;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

/**
 * Created by mike on 1/23/18.
 */
@Data
@Accessors(prefix = "_")
@EqualsAndHashCode(callSuper = true)
@AllArgsConstructor
@NoArgsConstructor
@MappedSuperclass
public class PacsRequest extends AbstractHibernateEntity implements Serializable {
    public static final String QUEUED_STATUS_TEXT     = "QUEUED";
    public static final String PROCESSING_STATUS_TEXT = "PROCESSING";
    public static final String ISSUED_STATUS_TEXT     = "ISSUED";
    public static final String FAILED_STATUS_TEXT     = "FAILED";
    public static final String RECEIVED_STATUS_TEXT   = "RECEIVED";
    public static final Long   HIGH_PRIORITY          = 1L;
    public static final Long   STANDARD_PRIORITY      = 10L;

    public void setSeriesIds(final String seriesIds) {
        _seriesIds = seriesIds;
    }

    public void setSeriesIds(final List<String> seriesIds) {
        _seriesIds = StringUtils.join(seriesIds, ", ");
    }

    @Override
    public String toString() {
        return "{ username: " + getUsername() + ", "
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

    private String _username;
    private Long   _pacsId;
    private String _xnatProject;
    private String _studyInstanceUid;
    @Column(columnDefinition = "TEXT")
    private String _seriesIds;
    @Column(columnDefinition = "TEXT")
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
