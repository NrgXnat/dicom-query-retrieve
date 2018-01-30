package org.nrg.dqr.services;

import org.nrg.dqr.domain.entities.PacsPing;
import org.nrg.dqr.domain.entities.QueuedPacsRequest;
import org.nrg.framework.orm.hibernate.BaseHibernateService;
import org.nrg.xft.security.UserI;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
public interface PacsPingService extends BaseHibernateService<PacsPing> {
    PacsPing getLatestPing(Long pacsId);
    List<PacsPing> getPings(Long pacsId);
}
