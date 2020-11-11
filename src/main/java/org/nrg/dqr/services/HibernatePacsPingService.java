package org.nrg.dqr.services;

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.daos.PacsPingDAO;
import org.nrg.dqr.domain.entities.PacsPing;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
@Slf4j
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
