/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.*;
import javax.validation.constraints.NotBlank;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"projectId"}))
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@Audited
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectIrbInfo extends AbstractHibernateEntity implements Serializable {
    @NotBlank
    public String getProjectId() {
        return _projectId;
    }

    public void setProjectId(final String projectId) {
        _projectId = projectId;
    }

    public String getIrbNumber() {
        return _irbNumber;
    }

    public void setIrbNumber(final String irbNumber) {
        _irbNumber = irbNumber;
    }

    @OneToMany(targetEntity = ProjectIrbFile.class, cascade = CascadeType.ALL)
    public List<ProjectIrbFile> getProjectIrbFiles() {
        return new ArrayList<>(_projectIrbFiles);
    }

    public void setProjectIrbFiles(final List<ProjectIrbFile> projectIrbFiles) {
        _projectIrbFiles.clear();
        _projectIrbFiles.addAll(projectIrbFiles);
    }

    public void addIrbFile(final ProjectIrbFile file) {
        _projectIrbFiles.add(file);
    }

    public void addIrbFile(final String fileName, final byte[] bytes) {
        _projectIrbFiles.add(new ProjectIrbFile(this, fileName, bytes));
    }

    @Override
    public String toString() {
        return "IrbInfo{" +
               "projectId='" + _projectId + '\'' +
               ", irbNumber='" + _irbNumber + '\'' +
               ", irbFiles='" + _projectIrbFiles.stream().map(ProjectIrbFile::getIrbFileName).collect(Collectors.joining(", ")) + '\'' +
               '}';
    }

    private String _projectId;
    private String _irbNumber;

    @Singular
    private final List<ProjectIrbFile> _projectIrbFiles = new ArrayList<>();
}
