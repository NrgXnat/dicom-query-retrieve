package org.nrg.xnatx.dqr.services;

import org.nrg.xnatx.dqr.domain.entities.PacsPing;
import org.nrg.framework.orm.hibernate.BaseHibernateService;

import java.util.List;

/**
 * Created by mike on 1/19/18.
 */
public interface PacsPingService extends BaseHibernateService<PacsPing> {
    PacsPing getLatestPing(Long pacsId);
    List<PacsPing> getPings(Long pacsId);
}
