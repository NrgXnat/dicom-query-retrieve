package org.nrg.xnatx.dqr.services.impl.hibernate;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.xnatx.dqr.domain.daos.PacsPingDAO;
import org.nrg.xnatx.dqr.domain.entities.PacsPing;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xnatx.dqr.services.PacsPingService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.inject.Inject;
import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
@Service
public class HibernatePacsPingService extends AbstractHibernateEntityService<PacsPing, PacsPingDAO> implements PacsPingService {

    private static final Log _log = LogFactory.getLog(HibernatePacsPingService.class);

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
