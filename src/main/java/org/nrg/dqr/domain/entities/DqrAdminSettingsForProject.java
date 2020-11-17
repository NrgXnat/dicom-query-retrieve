package org.nrg.dqr.domain.entities;

import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

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
@AllArgsConstructor
@NoArgsConstructor
public class DqrAdminSettingsForProject extends AbstractHibernateEntity {
    public String getProjectId() {
        return _projectId;
    }

    public void setProjectId(final String projectId) {
        _projectId = projectId;
    }

    private String _projectId;
}
