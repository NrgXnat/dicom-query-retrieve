/*
 * dicom-query-retrieve: org.nrg.xnatx.dqr.services.impl.hibernate.HibernatePacsPingService
 * XNAT http://www.xnat.org
 * Copyright (c) 2005-2020, Washington University School of Medicine
 * All Rights Reserved
 *
 * Released under the Simplified BSD.
 */

package org.nrg.xnatx.dqr.services.impl.hibernate;

import java.util.List;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.domain.daos.PacsPingDAO;
import org.nrg.xnatx.dqr.domain.entities.PacsPing;
import org.nrg.xnatx.dqr.services.PacsPingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernatePacsPingService extends AbstractHibernateEntityService<PacsPing, PacsPingDAO> implements PacsPingService {
    @Override
    @Transactional
    public PacsPing getLatestPing(final long pacsId) {
        return getDao().getLatestPing(pacsId);
    }

    @Override
    @Transactional
    public List<PacsPing> getPings(final long pacsId) {
        return getDao().getPings(pacsId);
    }
}
