package org.nrg.dqr.domain.entities;

import lombok.extern.slf4j.Slf4j;
import org.hibernate.annotations.Cache;
import org.hibernate.annotations.CacheConcurrencyStrategy;
import org.hibernate.annotations.Fetch;
import org.hibernate.annotations.FetchMode;
import org.hibernate.envers.Audited;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;
import java.util.Set;

/**
 * Created by mike on 1/23/18.
 */
@Slf4j
@Entity
@Table
@Audited
@Cache(usage = CacheConcurrencyStrategy.READ_WRITE, region = "nrg")
public class DqrAdminSettingsForProject extends AbstractHibernateEntity implements Serializable {
    public DqrAdminSettingsForProject(){

    }

    public DqrAdminSettingsForProject(String _projectId) {
        this._projectId = _projectId;
    }

    public String getProjectId() {
        return _projectId;
    }

    public void setProjectId(String _projectId) {
        this._projectId = _projectId;
    }

    protected static final long serialVersionUID = 1L;

    protected String _projectId;
}
