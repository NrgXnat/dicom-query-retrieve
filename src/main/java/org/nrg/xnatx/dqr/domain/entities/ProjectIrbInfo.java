/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.ProjectIrbInfo
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.Singular;
import org.apache.commons.lang3.builder.EqualsBuilder;
import org.apache.commons.lang3.builder.HashCodeBuilder;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;
import org.nrg.xnat.entities.FileStoreInfo;

import javax.annotation.Nonnull;
import javax.persistence.*;
import org.hibernate.validator.constraints.NotBlank;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Entity
@Table(uniqueConstraints = @UniqueConstraint(columnNames = {"projectId"}))
@Cacheable
@Audited
@NoArgsConstructor
public class ProjectIrbInfo extends AbstractHibernateEntity implements Serializable {
    private static final long serialVersionUID = -7219961069239647732L;

    @Builder
    public ProjectIrbInfo(final String projectId, final String irbNumber, final @Singular List<FileStoreInfo> projectIrbFiles) {
        _projectId = projectId;
        _irbNumber = irbNumber;
        _projectIrbFiles.addAll(projectIrbFiles);
    }

    @NotBlank
    @Nonnull
    public String getProjectId() {
        return _projectId;
    }

    public void setProjectId(final @Nonnull String projectId) {
        _projectId = projectId;
    }

    @NotBlank
    @Nonnull
    public String getIrbNumber() {
        return _irbNumber;
    }

    public void setIrbNumber(final @Nonnull String irbNumber) {
        _irbNumber = irbNumber;
    }

    @OneToMany(targetEntity = FileStoreInfo.class, cascade = CascadeType.ALL)
    public List<FileStoreInfo> getProjectIrbFiles() {
        return new ArrayList<>(_projectIrbFiles);
    }

    public void setProjectIrbFiles(final List<FileStoreInfo> projectIrbFiles) {
        _projectIrbFiles.clear();
        _projectIrbFiles.addAll(projectIrbFiles);
    }

    public void addIrbFile(final FileStoreInfo file) {
        _projectIrbFiles.add(file);
    }

    @Override
    public String toString() {
        return "IrbInfo{" +
               "projectId='" + _projectId + '\'' +
               ", irbNumber='" + _irbNumber + '\'' +
               ", irbFiles='" + _projectIrbFiles.stream().map(FileStoreInfo::getLabel).collect(Collectors.joining(", ")) + '\'' +
               '}';
    }

    @Override
    public boolean equals(final Object other) {
        if (this == other) {
            return true;
        }

        if (other == null || getClass() != other.getClass()) {
            return false;
        }

        final ProjectIrbInfo that = (ProjectIrbInfo) other;

        return new EqualsBuilder()
                .append(getId(), that.getId())
                .append(getProjectId(), that.getProjectId())
                .append(getIrbNumber(), that.getIrbNumber())
                .append(getProjectIrbFiles(), that.getProjectIrbFiles())
                .isEquals();
    }

    @Override
    public int hashCode() {
        return new HashCodeBuilder(17, 37)
                .append(getId())
                .append(getProjectId())
                .append(getIrbNumber())
                .append(getProjectIrbFiles())
                .toHashCode();
    }

    private String _projectId;

    private String _irbNumber;

    @Singular
    private final List<FileStoreInfo> _projectIrbFiles = new ArrayList<>();
}
