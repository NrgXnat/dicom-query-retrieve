package org.nrg.dqr.services;

import lombok.extern.slf4j.Slf4j;
import org.nrg.dqr.daos.PacsPingDAO;
import org.nrg.dqr.domain.entities.PacsPing;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
@Slf4j
public class HibernatePacsPingService extends AbstractHibernateEntityService<PacsPing, PacsPingDAO> implements PacsPingService {
    @Inject
    private PacsPingDAO _dao;

    @Override
    @Transactional
    public PacsPing getLatestPing(Long pacsId) {
        return _dao.getLatestPing(pacsId);
    }

    @Override
    @Transactional
    public List<PacsPing> getPings(Long pacsId) {
        return _dao.getPings(pacsId);
    }
}
