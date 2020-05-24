/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.ProjectIrbFile
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.*;
import javax.validation.constraints.NotNull;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"irbFileName"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@Audited
@AllArgsConstructor
@NoArgsConstructor
public class ProjectIrbFile extends AbstractHibernateEntity {
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @NotNull
    public ProjectIrbInfo getProjectIrbInfo() {
        return _projectIrbInfo;
    }

    public void setProjectIrbInfo(final ProjectIrbInfo projectIrbInfo) {
        _projectIrbInfo = projectIrbInfo;
    }

    @NotNull
    public String getIrbFileName() {
        return _irbFileName;
    }

    public void setIrbFileName(final String irbFileName) {
        _irbFileName = irbFileName;
    }

    @NotNull
    @Lob
    public byte[] getIrbFile() {
        return _irbFile;
    }

    public void setIrbFile(final byte[] irbFile) {
        _irbFile = irbFile;
    }

    private ProjectIrbInfo _projectIrbInfo;
    private String         _irbFileName;
    private byte[]         _irbFile;
}
