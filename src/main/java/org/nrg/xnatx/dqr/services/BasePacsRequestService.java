/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.BasePacsRequestService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services;

import java.util.List;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;
import org.nrg.xnatx.dqr.domain.entities.PacsRequest;

public interface BasePacsRequestService<R extends PacsRequest> extends BaseHibernateService<R> {
    List<R> getAllForUser(final UserI user);

    R getByIdForUser(final long id, final UserI user);
}
