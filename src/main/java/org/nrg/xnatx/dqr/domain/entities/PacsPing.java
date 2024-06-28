/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.entities.PacsPing
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.entities;

import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntity;

import javax.persistence.Cacheable;
import javax.persistence.Entity;
import javax.persistence.Table;
import java.io.Serializable;
import java.util.Date;

@Entity
@Table
@Cacheable
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class PacsPing extends AbstractHibernateEntity implements Serializable {
    private static final long   serialVersionUID = 4404214285524614283L;
    private static final String TO_STRING_FORMAT = "{\n  pacsId: %d,\n  successful: %b,\n  pingTime: %Tc\n}";

    protected Long    _pacsId;
    protected boolean _successful;
    protected Date    _pingTime;

    public Long getPacsId() {
        return _pacsId;
    }

    public void setPacsId(final Long pacsId) {
        _pacsId = pacsId;
    }

    public boolean isSuccessful() {
        return _successful;
    }

    public void setSuccessful(final boolean successful) {
        _successful = successful;
    }

    public Date getPingTime() {
        return _pingTime;
    }

    public void setPingTime(final Date pingTime) {
        _pingTime = pingTime;
    }

    @Override
    public String toString() {
        return String.format(TO_STRING_FORMAT, _pacsId, _successful, _pingTime);
    }
}
