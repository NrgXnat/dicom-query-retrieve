/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.AbstractHibernatePacsRequestService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import java.util.List;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.daos.AbstractPacsRequestDAO;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;
import org.nrg.xnatx.dqr.services.BasePacsRequestService;
import org.springframework.transaction.annotation.Transactional;

@Transactional
public abstract class AbstractHibernatePacsRequestService<R extends PacsRequest, DAO extends AbstractPacsRequestDAO<R>> extends AbstractHibernateEntityService<R, DAO> implements BasePacsRequestService<R> {
    @Override
    public List<R> getAllForUser(final UserI user) {
        return getDao().findAllForUser(user);
    }

    @Override
    public R getByIdForUser(final long id, final UserI user) {
        return getDao().findByIdForUser(id, user);
    }
}
