/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.domain.daos.AbstractPacsRequestDAO
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.domain.daos;

import java.util.List;
import org.hibernate.criterion.Restrictions;
import org.nrg.framework.orm.hibernate.AbstractHibernateDAO;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;

public abstract class AbstractPacsRequestDAO<R extends PacsRequest> extends AbstractHibernateDAO<R> {
    public List<R> findAllForUser(final UserI user) {
        return findByCriteria(Restrictions.eq("username", user.getUsername()));
    }

    public R findByIdForUser(final long id, final UserI user) {
        return instance(findByCriteria(Restrictions.eq("id", id), Restrictions.eq("username", user.getUsername())));
    }
}
