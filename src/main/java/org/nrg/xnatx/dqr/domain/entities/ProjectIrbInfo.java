/*
 * Pacs
 * DQR is developed by the Neuroinformatics Research Group
 * XNAT http://www.xnat.org
 * Copyright (c) 2013, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 *
 * Last modified 9/24/13 6:11 PM
 */

package org.nrg.xnatx.dqr.domain.entities;

import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.hibernate.envers.Audited;
import javax.persistence.Entity;
import javax.persistence.Lob;
import javax.persistence.Table;
import javax.persistence.UniqueConstraint;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"projectId"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@Audited
public class ProjectIrbInfo extends AbstractHibernateEntity implements Serializable {

    private static final long serialVersionUID = 1L;

    private String _projectId;
    private String _irbNumber;
    private String _irbFileName;

    @Lob
    private byte[] _irbFile;

    // for Hibernate
    public ProjectIrbInfo() {
    }

    @NotBlank
    public String getProjectId() {
        return _projectId;
    }

    public void setProjectId(String _projectId) {
        this._projectId = _projectId;
    }

    public String getIrbNumber() {
        return _irbNumber;
    }

    public void setIrbNumber(String _irbNumber) {
        this._irbNumber = _irbNumber;
    }

    public byte[] getIrbFile() {
        return _irbFile;
    }

    public void setIrbFile(byte[] _irbFile) {
        this._irbFile = _irbFile;
    }

    public String getIrbFileName() {
        return _irbFileName;
    }

    public void setIrbFileName(String _irbFileName) {
        this._irbFileName = _irbFileName;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        ProjectIrbInfo that = (ProjectIrbInfo) o;

        if (!_projectId.equals(that._projectId)) return false;
        if (!_irbNumber.equals(that._irbNumber)) return false;
        if (!_irbFile.equals(that._irbFile)) return false;
        return _irbFileName.equals(that._irbFileName);
    }

    @Override
    public int hashCode() {
        int result = new HashCodeBuilder(13, 137)
            .append(_projectId)
            .append(_irbNumber)
            .append(_irbFile)
            .append(_irbFileName).toHashCode();
        return result;
    }

    @Override
    public String toString() {
        return "ProjectIrbInfo{" +
                "projectId='" + _projectId + '\'' +
                ", irbNumber='" + _irbNumber + '\'' +
                ", irbFileName='" + _irbFileName + '\'' +
                '}';
    }
}
