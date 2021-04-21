/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.DqrProjectSettings
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import java.io.Serializable;
import javax.persistence.Entity;
import javax.persistence.Table;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
@Entity
@Table
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class DqrProjectSettings extends AbstractHibernateEntity implements Serializable {
    private static final long serialVersionUID = -8377837327063450758L;

    public DqrProjectSettings() {
        //
    }

    public DqrProjectSettings(final String projectId) {
        _projectId = projectId;
    }

    public String getProjectId() {
        return _projectId;
    }

    public void setProjectId(final String projectId) {
        _projectId = projectId;
    }

    protected String _projectId;
}
