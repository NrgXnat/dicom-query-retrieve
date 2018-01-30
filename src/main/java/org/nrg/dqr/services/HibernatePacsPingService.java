package org.nrg.dqr.services;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.nrg.dqr.daos.ExecutedPacsRequestDAO;
import org.nrg.dqr.daos.PacsPingDAO;
import org.nrg.dqr.domain.entities.ExecutedPacsRequest;
import org.nrg.dqr.domain.entities.PacsPing;
import org.nrg.framework.orm.hibernate.AbstractHibernateEntityService;
import org.nrg.xft.security.UserI;
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
