/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.DqrProjectSettings
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
import lombok.experimental.Accessors;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Table;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
@Entity
@Table
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
@Accessors(prefix = "_")
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class DqrProjectSettings extends AbstractHibernateEntity implements Serializable {
    private static final long serialVersionUID = -8377837327063450758L;

    private String  _projectId;
    private boolean _dqrEnabled;

    public String getProjectId() {
        return _projectId;
    }

    public void setProjectId(final String projectId) {
        _projectId = projectId;
    }

    public boolean isDqrEnabled() {
        return _dqrEnabled;
    }

    public void setDqrEnabled(final boolean dqrEnabled) {
        _dqrEnabled = dqrEnabled;
    }
}
